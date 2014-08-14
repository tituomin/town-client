(ns town-client.macros
  (:require
   [net.cgrand.enlive-html :refer [attr-has]]
   [town-client.config :refer [master-template]]
   [kioo.reagent :refer [defsnippet deftemplate]]))


(defmacro defstatvisualisation [name path selector keys]
  (let [data-arg (gensym)]
    `(defsnippet ~name ~(eval path) ~selector [~data-arg]
       ~(into
         {} (map (fn [a]
                   `[[[:.choice-line ~(attr-has :data-choice (clojure.core/name `~a))]]
                    (kioo.reagent/set-attr
                     :style {:width (clojure.string/join
                                     [(clojure.core/get (clojure.core/deref ~data-arg) ~a) "%"])})])
                 keys)))))

