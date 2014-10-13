(ns town-client.state
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljs.core.async :as async]
   [town-client.intents :as intents]
   [clojure.zip :as zip]
   [town-client.language :as language]
   [town-client.config :refer [aggregates]]))

(def app-state
  {:future-accommodation (atom {})
   :family-situation (atom {})
   :transport-preferences (atom {})
   :age (atom {})
   :opinions (atom {})
   :debug (atom {})})

(def neighborhoods
  (atom {0 {:name "ladataan"
            :respondent-count 0
            :id 0
            :prev nil
            :next nil}}))

(def rankings
  (atom (into
         {} (map (fn [pair] [(first pair) {}])
                 aggregates))))

(def current-neighborhood
  (atom {:genetive "Kenen"}))
(def selected-neighborhood
  (atom {}))

(def search
  (atom {:results []
         :input nil
         :index nil
         :selected -1}))

(def mouse-cursor
  (atom {:y 0 :x 0}))
(defn set-cursor [{:keys [x y]}]
  (swap! mouse-cursor assoc :x x :y y))

(def visualisation-key
  { "joukkoliikenne" :public
    "henkiloauto" :car
    "kavely" :walk
    "polkupyora" :bike
    "pariskunta" :couple
    "yksin-asuva" :single
    "lapsiperhe" :withkids
    "yhteiso" :group
    "muu" :other
    "erittain-todennakoisesti" :verylikely
    "melko-todennakoisesti" :quitelikely
    "melko-epatodennakoisesti" :quiteunlikely
    "erittain-epatodennakoisesti" :veryunlikely
    "vaikea-sanoa" :notsure
    })

(def scale-keys
  #{:agree_add_density
   :agree_add_my_area_density_for_less_cars
   :agree_bulevardisation
   :agree_high_rise
   :agree_suburbs_build_near_stations
   :enjoy_culture_urban_meetings
   :enjoy_metropolis_fascinating_possibilities
   :enjoy_outdoors_large_woods
   :my_area_could_be_built_more
   :prefer_daily_shopping_near
   :would_use_rail_transport_more
   })

(def visualisation-group-key
  { "age_low" :age
    "transport_mode_first" :transport-preferences
    "life_situation" :family-situation
    "probability_stay_five_years" :future-accommodation
   })

(defn scale-key [data-key]
  (keyword (clojure.string/replace data-key "scale_" "")))

(defn process-stats [respondent-count answers scales]
  ; Clear any previous results so keys missing from current
  ; neighborhood do not persist.
  (doseq [group-key (keys answers)]
    (reset! (app-state (visualisation-group-key group-key)) {}))
  (reset! (app-state :opinions) {})
  (doseq [[group-key group-values] answers
          [key value] group-values]
    (swap! current-neighborhood assoc :respondent-count respondent-count)
    (swap! (app-state (visualisation-group-key group-key))
           assoc (or (visualisation-key key)
                     (keyword (str key)))
           (/ (* 100 value) respondent-count)))
  (reset! (app-state :opinions)
          (into {} (map (fn [[k v]] [(scale-key k) v])) scales)))


(defn process-neighborhood [neighborhood]
  (let [id (neighborhood "id")
        genetive-form (language/citizen-genetive ((neighborhood "name") "fi"))]
    (swap! current-neighborhood assoc
           :id id
           :genetive genetive-form
           :next (:next (@neighborhoods id))
           :prev (:prev (@neighborhoods id)))))

(defn process-map-stats
  [stats key]
  (if key
      (swap!
       rankings
       assoc key
       (take 5 (sort-by (fn [x] (- (:count x))) stats)))))

(defn zip-map [f loc]
  " Map f over every node of the zipper.
	    The function received has the form (f node-value loc),
	    the node value and its location"
  (loop [z loc]
    (if (zip/end? z)
      (zip/root z) ; perhaps you can call zip/seq-zip or zip/vector-zip?
      (recur (zip/next (zip/edit z f z))))))

(defn coords-to-google [coords]
  (zip-map
   (fn [n nx]
     (if (vector? (first n)) n
         #js{:lng (first n) :lat (second n)}))
   (zip/vector-zip coords)))

(defn vector-to-array [el]
  (if (vector? el)
      (to-array (map vector-to-array el))
      el))

(defn replacer [from to]
  (fn [s] (clojure.string/replace s from to)))

(def filters
  [clojure.string/lower-case]
  #_(conj (apply vector
               (map #(apply replacer %)
                    [["å" "a"]
                     ["ä" "a"]
                     ["ö" "o"]]))
        clojure.string/lower-case))

(defn tokenize
  [s] (clojure.string/split s #"[- ]"))

(defn index-neighborhoods [neighborhoods]
  (into [] (for [n neighborhoods
                 token (tokenize (:name n))]
             [((apply comp filters) token) (:id n)])))

(defn index-find [index query]
  (let [tokens
        (map (apply comp filters)
             (tokenize query))]
    (apply clojure.set/intersection (for [token tokens]
      (set (map second (index-find-token index token)))))))

(defn index-find-token [index substr]
  (filter #(re-find (re-pattern (str "^" substr)) (first %)) index))

(defn search-neighborhoods [input user-channel]
  (let [results
        (for [nid (index-find (@search :index) input)]
          (@neighborhoods nid))]
    (if (and
         #_(> (count input) 1)
         (< (count results) 2)
         (<= (count input) (count (@search :input)))
         #_(or (= [] (@search :results))
             (<= (count results) (count (@search :results)))))
      (swap! search assoc :input input :results [])
      (do (swap! search assoc :results (sort-by :name results) :input input :selected -1)
          (if (= 1 (count results))
            (async/put! user-channel
                        (intents/highlight-neighborhood
                         (:id (first results)) :menu))
            (async/put! user-channel
                        (intents/highlight-neighborhood
                         nil :menu)))))))

(defn select-index [user-channel next-fn cmp-fn cmp-val]
  (swap! search
        (fn [val]
          (let [next-sel (cmp-fn cmp-val (next-fn (:selected val)))
                sel-id (:id (nth (:results val) next-sel))]
            (async/put! user-channel (intents/highlight-neighborhood sel-id :menu))
            (assoc val :selected next-sel)))))

(defn select-next [user-channel]
  (select-index user-channel inc min (dec (count (:results @search)))))

(defn select-prev [user-channel]
  (select-index user-channel dec max 0))
