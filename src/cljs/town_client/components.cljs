(ns town-client.components
  (:require
   [town-client.util :refer [log-v log]]
   [clojure.string]
   [kioo.core]
   [kioo.reagent :refer [content set-attr do-> substitute listen append]]
   [reagent.core :as reagent :refer [atom]])
  (:require-macros [kioo.reagent :refer [defsnippet deftemplate]]
                   [town-client.macros :refer [defstatvisualisation]]))

(def template-path "public/kaupunginosa/index.html")
(def mock-neighborhoods [["kallio" 1] ["herttoniemi" 2] ["alppila" 3]])
(def mock-neighborhood-genetive "Kalliolaisten")

(defsnippet neighborhood-menu-item
  "public/kaupunginosa/index.html"
  [:select.navigate-areas [:option first-of-type]]
  [[name id]]
  {[root] (do-> (content name) (set-attr :value id))})

(defsnippet neighborhood-menu
  "public/kaupunginosa/index.html"
  [:select.navigate-areas]
  [neighborhoods]
  {[root] (content (map neighborhood-menu-item neighborhoods))})

(defstatvisualisation future-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.future]]
  [:verylikely :quitelikely :notsure :quiteunlikely :veryunlikely])


;; (defstatvisualisation family-visualisation
;;   town-client.config/master-template
;;   [:.choice-graphs [:.g-choice :.family]]
;;   []
;;   [["single" 0] ["couple" 10] ["withkids" 20] ["group" 30] ["other" 40]])

;; (defstatvisualisation transport-visualisation
;;   town-client.config/master-template
;;   [:.choice-graphs [:.g-choice :.transport]]
;;   []
;;   [["car" 100] ["bike" 50] ["walk" 20] ["public" 100]])

;; (defstatvisualisation age-visualisation
;;   town-client.config/master-template
;;   [:.choice-graphs [:.g-choice :.age]]
;;   []
;;   [[0 80] [16 70] [20 60] [25 50] [30 40] [40 50] [50 60] [60 70] [70 80]])

(defsnippet background-info-section
  "public/kaupunginosa/index.html" 
  [[:.g-info-section :.background]] []
  {[:.choice-graphs [:.g-choice :.future]] (substitute
                                            (future-visualisation
                                             {:verylikely 0
                                              :quitelikely 10
                                              :notsure 20
                                              :quiteunlikely 40
                                              :veryunlikely 80}))
   ;; [:.choice-graphs [:.g-choice :.family]] (substitute (family-visualisation))
   ;; [:.choice-graphs [:.g-choice :.transport]] (substitute (transport-visualisation))
   ;; [:.choice-graphs [:.g-choice :.age]] (substitute (age-visualisation))
   })

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
   (substitute (background-info-section))
})

#_(
   (reagent/render-component [head "Kenen kaupunki"]
                             (first (.getElementsByTagName js/document "head")))
   (reagent/render-component [page]
                             (.getElementById js/document "content-wrap"))
  )
