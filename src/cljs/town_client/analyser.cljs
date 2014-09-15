(ns town-client.analyser
  (:require [town-client.state :as state]))

(def data (atom {}))

(defn counts
  [values]
  (reduce
   (fn [results key]
     (let [key     (or key 0)
           current (or (results key) 0)]
       (assoc results key (inc current))))
    {} values))

(defn enum-proportions [respondents key]
    (counts (map #(% key) respondents)))

(defn map-stats
  [data neighborhoods]
  (map (fn [[k v]]
         {:name ((neighborhoods k) :name) :count v})
       (counts (filter (set (keys neighborhoods))
                       (flatten (map #((% "properties") "divisions")
                                     (flatten (map #(% "features")
                                                   (map js->clj
                                                        (vals
                                                         (:results data)))))))))))

(defn summer [keys]
  (fn [mapx mapy]
    (into {} (map
     #(vector % (+ (mapx %) (mapy %)))
       keys))))

(defn scale-string [data-key]
  (str "scale_" (name data-key)))

(defn totals [data]
  (let [scale-keys (map scale-string state/scale-keys)
        reducer (summer scale-keys)]
    (reduce reducer data)))

(defn divide [divisor [k v]]
  [k (/ v divisor)])

(defn averages [data]
  (into {}
        (map (partial divide (count data))
             (totals data))))

(defn scale-key [data-key]
  (keyword (clojure.string/replace data-key "scale_" "")))

