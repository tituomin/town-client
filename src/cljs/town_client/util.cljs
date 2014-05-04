(ns town-client.util)

(defn log [msg] (.log js/console msg))
(defn log-v [v] (.log js/console (pr-str v)))

(defn make-js-object
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)
        out-wrap (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    (set! (.-data out-wrap) out)
    out-wrap))
