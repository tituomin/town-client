(ns town-client.util)

(defn log-internal [message]
  (if-not (undefined? js/window.console)
    (.log js/console message)))

(defn log [msg] (log-internal msg))
(defn log-v [v] (log-internal (pr-str v)))

(defn make-js-object
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)
        out-wrap (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    (set! (.-data out-wrap) out)
    out-wrap))
