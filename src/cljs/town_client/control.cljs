(ns town-client.control
  (:require
   [clojure.set]
   [goog.events]
   [town-client.config :refer [aggregates]]
   [town-client.components :as components]
   [town-client.data :refer [fetch-data]]
   [town-client.state :as state]
   [town-client.intents :as intents]
   [town-client.util :refer [log-v log]]
   [town-client.templating :as tmpl]
   [town-client.map :as map]
   [town-client.analyser :as analyser]
   [cljs.core.async :as async
    :refer [<! >! filter< chan close! sliding-buffer put! mult tap alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn fetch-all-neighborhoods [channel]
  (fetch-data channel "divisions" {:type 4 :geometry "true"}))
(defn fetch-neighborhood [channel id]
  (fetch-data channel "divisions" {:geometry "true"} id))
(defn fetch-respondents [channel nid]
  (fetch-data channel "respondents" {:neighborhood nid :page_size 500}))
(defn fetch-answers [channel nid category]
  (fetch-data channel "answers" {:category category :respondent__neighborhood nid}))

(defn handle-user-input [intent data-channel]
  (condp = (:type intent)
    :newlocation
    (let [nid (:location intent)]
      (components/init)
      (fetch-neighborhood data-channel nid)
      (fetch-respondents data-channel nid)
      (doseq [x (flatten (map seq (vals aggregates)))]
        (fetch-answers data-channel nid x)))
    :mousemove
    (state/set-cursor intent)
    :highlight-neighborhood
    (do
      (reset! state/current-neighborhood
            (assoc (@state/neighborhoods (:id intent))
              :source (:source intent))))))

(defn category-to-map-id
  [category]
  (first (first (filter (fn [[k v]] (v category)) aggregates))))

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
                  #(and (not (= "Aluemeri" (:name %))) (not (= "Ulkosaaret" (:name %))))
                  (map
                   (fn [n] {:name ((n "name") "fi") :id (n "id") :geometry (n "boundary")})
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

(defn handle-new-data [data incomplete-channel user-channel map-user-channel]
  (case (:resource-type data)
    :divisions
    (if (< 1 (count (:results data)))
      ; no id, got all neighborhoods
      (do
        (reset! state/neighborhoods (process-neighborhoods (:results data)))
        (swap! state/search assoc :index
                (state/index-neighborhoods (vals @state/neighborhoods)))
        ; todo: move init hash handling elsewhere?
        (let [init-hash (-> js/window .-location .-hash)]
          (when (not (clojure.string/blank? init-hash))
            (async/put!
             user-channel
             (intents/neighborhood-intent (subs init-hash 1)))))
        (map/add-neighborhood-map! "neighborhood-map" user-channel map-user-channel))
      ; specific neighborhood
    (state/process-neighborhood (first (:results data))))

    :respondents
    (state/process-stats
     (count (:results data))
     (into {} (for [key (keys state/visualisation-group-key)]
                [key (analyser/enum-proportions (:results data) key)]))
     (analyser/averages (:results data)))

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

(defn init [user-channel-original]
  (let [data-channel (chan)
        incomplete-channel (chan)
        user-channel (chan)
        map-user-chan (chan)
        user-mult (mult user-channel-original)]

    (tap user-mult map-user-chan)
    (tap user-mult user-channel)

    (.listen goog.events
             js/window "hashchange"
             #(async/put! user-channel-original
                          (intents/neighborhood-intent
                           (subs
                          (-> % .-currentTarget
                              .-location .-hash) 1))))
    (set! (.-onmousemove js/window)
             (fn [e]
               #_(.log js/console e)
               (async/put!
                user-channel-original
               {:type :mousemove :x (.-pageX e) :y (.-pageY e)})
               ))

    (fetch-all-neighborhoods data-channel)
    (receive-partial-data incomplete-channel data-channel)
    (go 
      (while true
        (let [[value c] (alts! [user-channel data-channel])]
          (condp = c
            data-channel (handle-new-data value incomplete-channel user-channel-original map-user-chan)
            user-channel (handle-user-input value data-channel)))))))
