(ns town-client.components
  (:require
   [town-client.util :refer [log-v log]]
   [kioo.reagent :refer [content set-attr do-> substitute listen append]]
   [reagent.core :as reagent :refer [atom]])
  (:require-macros [kioo.reagent :refer [defsnippet deftemplate]]
                   [town-client.macros :refer [defstatvisualisation]]))

(def template-path "public/kaupunginosa/index.html")
(def mock-neighborhoods [["kallio" 1] ["herttoniemi" 2] ["alppila" 3]])
(def mock-neighborhood-genetive "Kalliolaisten")

(defsnippet neighborhood-menu-item "public/kaupunginosa/index.html"
  [:select.navigate-areas [:option first-of-type]] [[name id]]
  {[root] (do-> (content name) (set-attr :value id))})

(defsnippet neighborhood-menu "public/kaupunginosa/index.html"
  [:select.navigate-areas] [neighborhoods]
  {[root] (content (map neighborhood-menu-item neighborhoods))})

(defsnippet future-visualization "public/kaupunginosa/index.html"
  [:.choice-graphs [:.g-choice :.future]] []
  {[[:.choice-line (attr-has :data-choice "verylikely")]]    (set-attr :style {:width "0%"})
   [[:.choice-line (attr-has :data-choice "quitelikely")]]   (set-attr :style {:width "10%"})
   [[:.choice-line (attr-has :data-choice "notsure")]]       (set-attr :style {:width "20%"})
   [[:.choice-line (attr-has :data-choice "quiteunlikely")]] (set-attr :style {:width "30%"})
   [[:.choice-line (attr-has :data-choice "veryunlikely")]]  (set-attr :style {:width "40%"})})
(defsnippet family-visualization "public/kaupunginosa/index.html"
  [:.choice-graphs [:.g-choice :.family]] []
  {[[:.choice-line (attr-has :data-choice "single")]]        (set-attr :style {:width "0%"})
   [[:.choice-line (attr-has :data-choice "couple")]]        (set-attr :style {:width "10%"})
   [[:.choice-line (attr-has :data-choice "withkids")]]      (set-attr :style {:width "20%"})
   [[:.choice-line (attr-has :data-choice "group")]]         (set-attr :style {:width "30%"})
   [[:.choice-line (attr-has :data-choice "other")]]         (set-attr :style {:width "40%"})})
(defsnippet transport-visualization "public/kaupunginosa/index.html"
  [:.choice-graphs [:.g-choice :.transport]] []
  {[[:.choice-line (attr-has :data-choice "car")]]           (set-attr :style {:width "0%"})
   [[:.choice-line (attr-has :data-choice "bike")]]          (set-attr :style {:width "10%"})
   [[:.choice-line (attr-has :data-choice "walk")]]          (set-attr :style {:width "20%"})
   [[:.choice-line (attr-has :data-choice "public")]]        (set-attr :style {:width "30%"})})

(defstatvisualisation age-visualization "public/kaupunginosa/index.html"
  [:.choice-graphs [:.g-choice :.age]] []
  [[0 80] [16 70] [20 60] [25 50] [30 40] [40 50] [50 60] [60 70] [70 80]])

(defsnippet background-info-section "public/kaupunginosa/index.html" 
  [[:.g-info-section :.background]] []
  {[:.choice-graphs [:.g-choice :.future]] (substitute (future-visualization))
   [:.choice-graphs [:.g-choice :.family]] (substitute (family-visualization))
   [:.choice-graphs [:.g-choice :.transport]] (substitute (transport-visualization))
   [:.choice-graphs [:.g-choice :.age]] (substitute (age-visualization))})


(defsnippet head "public/kaupunginosa/index.html" [:head] [neighborhood]
  {[:title] (content neighborhood)})

(deftemplate page "public/kaupunginosa/index.html" []
  {[:head]
   (substitute (head))
   [:.navigate-areas]
   (substitute (neighborhood-menu mock-neighborhoods))
   [:.header-area]
   (content mock-neighborhood-genetive)
   [[:.g-info-section :.background]]
   (substitute (background-info-section))})

#_(
  (reagent/render-component [head "Kenen kaupunki"] (first (.getElementsByTagName js/document "head")))
  (reagent/render-component [page] (.getElementById js/document "content-wrap"))
  )
