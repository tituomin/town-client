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
