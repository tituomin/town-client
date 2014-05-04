(ns town-client.analyser)

(def data (atom {}))

(defn counts
  [values]
  (reduce (fn [results value]
            (assoc results (or value 0)
                   (inc (or (results (or value 0)) 0))))
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
