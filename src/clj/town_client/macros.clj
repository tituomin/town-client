(ns town-client.macros
  (:require
   [net.cgrand.enlive-html :refer [attr-has]]
   [town-client.config :refer [master-template]]
   [kioo.reagent :refer [defsnippet deftemplate]]
   [kioo.core]))


(defmacro defstatvisualisation [name path selector container-class bar-class value-class keys]
  (let [data-arg (gensym)]
    `(defsnippet ~name ~(eval path) ~selector
       [~data-arg]
       ~(into
         {[:.summary-header]
          `(kioo.core/add-class
            (town-client.components/max-class
             (clojure.core/deref ~data-arg)))
          [:.summary-header :.summary-icon :i]
          `(kioo.core/set-class (town-client.components/max-icon
                                 (clojure.core/deref ~data-arg)))
          [:.summary-header :.summary-value]
          `(kioo.core/content (town-client.components/max-value
                                 (clojure.core/deref ~data-arg)))}
         (into
          (map (fn [a]
                   `[[[~container-class ~(attr-has :data-choice (clojure.core/name `~a))] ~bar-class]
                       (kioo.reagent/set-attr :style {:width (clojure.string/join
                           [(or (clojure.core/get
                                 (clojure.core/deref ~data-arg) ~a) "0") "%"])})])
                    keys)

          (map (fn [a]
                 `[[[~container-class ~(attr-has :data-choice (clojure.core/name `~a))] ~value-class]
                   (kioo.core/content (clojure.string/join
                                                  [(.toFixed (or (clojure.core/get
                                                        (clojure.core/deref ~data-arg) ~a) 0) 1) "%"]))]) keys))))))

