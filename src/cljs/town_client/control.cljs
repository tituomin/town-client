(ns town-client.control
  (:require
   [clojure.set]
   [goog.events]
   [town-client.config :refer [aggregates]]
   [town-client.components :as components]
   [town-client.data :refer [fetch-data]]
   [town-client.state :as state]
   [town-client.util :refer [log-v log]]
   [town-client.templating :as tmpl]
   [town-client.map :as map]
   [town-client.analyser :as analyser]
   [cljs.core.async :as async
    :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn fetch-all-neighborhoods [channel]
  (fetch-data channel "divisions" {:type 4}))
(defn fetch-neighborhood [channel id]
  (fetch-data channel "divisions" {:geometry "true"} id))
(defn fetch-respondents [channel nid]
  (fetch-data channel "respondents" {:neighborhood nid :page_size 500}))
(defn fetch-answers [channel nid category]
  (fetch-data channel "answers" {:category category :respondent__neighborhood nid}))

(defn handle-user-input [intent data-channel user-channel]
  (condp = (:type intent)
    :newlocation
    (let [nid (:location intent)]
      ;(tmpl/reinit-page user-channel)
      (fetch-neighborhood data-channel nid)
      (fetch-respondents data-channel nid)
      (doseq [x (flatten (map seq (vals aggregates)))]
        (fetch-answers data-channel nid x)))))

(defn category-to-map-id
  [category]
  (first (first (filter (fn [[k v]] (v category)) aggregates))))

(def app-state (atom {}))

(defn category [data]
  (if (or (nil? data) (empty? (data "features")))
    nil
    (((-> (data "features")
        first) "properties") "category")))

(defn process-neighborhoods [results]
  "'Simple' way of calculating the previous and next neighborhoods"
  (let [neighborhoods
        (sort-by #(:name %)
                 (filter
                  #(not (= "Aluemeri" (:name %)))
                  (map
                   (fn [n] {:name ((n "name") "fi") :id (n "id")})
                   results)))]
    (apply hash-map (flatten
              (for [neig
                (for [[e1 e2]
                  (partition 2
                    (apply conj (list {})
                      (reverse (conj (flatten 
                        (for [[left right] (partition 2 1 neighborhoods)]
                          [(assoc left :next (:id right))
                           (assoc right :prev (:id left))])) {}))))]
                  (merge e1 e2))]
                [(:id neig) neig])))))

(defn handle-new-data [data incomplete-channel user-channel]
  (case (:resource-type data)
    :divisions
    (if (< 1 (count (:results data)))
      ; no id, got all neighborhoods
      (do
        (reset! state/neighborhoods (process-neighborhoods (:results data)))
        ; todo: move init hash handling elsewhere?
        (let [init-hash (-> js/window .-location .-hash)]
          (when (not (clojure.string/blank? init-hash))
            (async/put!
             user-channel
             (neighborhood-intent (subs init-hash 1))))))
      ; specific neighborhood
    (state/process-neighborhood (first (:results data))))

    :respondents
    (state/process-stats
     (count (:results data))
     (into {} (for [key (keys state/visualisation-group-key)]
                [key (analyser/enum-proportions (:results data) key)])))

    :answer-aggregate
    (do
      (state/process-map-stats
       (analyser/map-stats data @state/neighborhoods) (:key data))
      (map/add-data data))

    :answers ; incomplete answers, to be aggregated
      (async/put! incomplete-channel (:results data))))


(defn receive-partial-data
  [channel complete-channel]
  (go
   (loop [received-keys #{}
          data {}]
     (let [val (<! channel)
           cdata              (js->clj val)
           category           (category cdata)
           aggregate-id       (keyword (category-to-map-id category))
           new-keys           (conj received-keys category)
           aggregate-keys     (aggregates aggregate-id)
           aggregate-complete? (clojure.set/subset? 
                               aggregate-keys
                               new-keys)
           new-data           (assoc-in data [aggregate-id category] val)]
       (if aggregate-complete?
         (do
           (>! complete-channel {:resource-type :answer-aggregate
                                  :key aggregate-id
                                  :results (new-data aggregate-id)})
           (recur (clojure.set/difference new-keys aggregate-keys)
                  (dissoc data aggregate-id)))
         (recur new-keys new-data))))))

(defn neighborhood-intent [id]
  {:type :newlocation, :location id })

(defn init []
  (let [data-channel (chan)
        user-channel (chan)
        incomplete-channel (chan)]
    (.listen goog.events
             js/window "hashchange"
             #(async/put! user-channel
                          (neighborhood-intent
                           (subs
                          (-> % .-currentTarget
                              .-location .-hash) 1))))
    
    (fetch-all-neighborhoods data-channel)
    (receive-partial-data incomplete-channel data-channel)
    (go 
      (while true
        (let [[value c] (alts! [user-channel data-channel])]
          (condp = c
            data-channel (handle-new-data value incomplete-channel user-channel)
            user-channel (handle-user-input value data-channel user-channel)))))))
