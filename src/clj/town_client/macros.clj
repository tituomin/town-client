(ns town-client.macros
  (:require
   [net.cgrand.enlive-html :refer [attr-has]]
   [town-client.config :refer [master-template]]
   [kioo.reagent :refer [defsnippet deftemplate]]
   [kioo.core]))


(defmacro defstatvisualisation [name path selector keys]
  (let [data-arg (gensym)]
    `(defsnippet ~name ~(eval path) ~selector [~data-arg]
       ~(into
         {[:.summary-header :.summary-icon :i]
          `(kioo.core/set-class (town-client.components/max-icon (clojure.core/deref ~data-arg)))}
         (map (fn [a]
                   `[[[:.choice-line ~(attr-has :data-choice (clojure.core/name `~a))]]
                    (kioo.reagent/set-attr
                     :style {:width (clojure.string/join
                                     [(or (clojure.core/get (clojure.core/deref ~data-arg) ~a) "0") "%"])})])
                 keys)))))

