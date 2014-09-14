(ns town-client.analyser)

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
  (map (fn [[k v]] {:name ((neighborhoods k) :name) :count v}) (counts (filter (set (keys neighborhoods))
                  (flatten (map #((% "properties") "divisions")
                                (flatten (map #(% "features")
                                              (map js->clj
                                                   (vals
                                                    (:results data)))))))))))

(defn map-summer [keys]
  (fn [mapx mapy]
    (into {} (map
     #(vector % (+ (mapx %) (mapy %)))
       keys))))

(defn totals [data]
  (let [reducer (map-summer [:a :b])]
    (reduce reducer data)))

(defn divide [divisor [k v]]
  [k (/ v divisor)])

(defn averages [data]
  (into {} (map (partial (divide (count data)))
                (totals data))))

(defn scale-key [data-key]
  (keyword (clojure.string/replace data-key "scale_" "")))

