(ns town-client.macros
  (:require
   [net.cgrand.enlive-html :refer [attr-has]]
   [kioo.reagent :refer [defsnippet deftemplate]]))

(defmacro defstatvisualisation [name path selector arguments data]
  `(defsnippet ~name ~path ~selector ~arguments
     ~(into
       {} (map (fn [[a b]]
                 [(vector (vector :.choice-line (attr-has :data-choice (str a))))
                  `(kioo.reagent/set-attr :style {:width ~(str b "%")})])
               data))))
