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

(defstatvisualisation family-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.family]]
  [:single :couple :withkids :group :other])

(defstatvisualisation transport-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.transport]]
  [:car :bike :walk :public])

(defstatvisualisation age-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.age]]
  [:0 :16 :20 :25 :30 :40 :50 :60 :70])

(def app-state
  {:future (atom {:verylikely 0
                  :quitelikely 10
                  :notsure 20
                  :quiteunlikely 40
;                  :veryunlikely 30
})})

(defsnippet background-info-section
  "public/kaupunginosa/index.html"
  [[:.g-info-section :.background]] []
  {[:.choice-graphs [:.g-choice :.future]]
   (substitute (future-visualisation (:future app-state)))
   [:.choice-graphs [:.g-choice :.family]]
   (substitute (family-visualisation
                (atom {:single 100
                 :couple 50
                 :withkids 100
                 :group 50
                 :other 0 })))
   ;; [:.choice-graphs [:.g-choice :.transport]]
   ;; (substitute (transport-visualisation
   ;;              {:car 100 :bike 90 :walk 80 :public 70}))
   ;; [:.choice-graphs [:.g-choice :.age]]
   ;; (substitute (age-visualisation
   ;;              {:0 50
   ;;               :16 40
   ;;               :20 60
   ;;               :25 20
   ;;               :30 80
   ;;               :40 0
   ;;               :50 100
   ;;               :60 0
   ;;               :70 100}))
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
   (reset! (:future app-state) {:verylikely 0
         :quitelikely 0
         :notsure 100
         :quiteunlikely 0
         :veryunlikely 100})
   (swap! (:future app-state) assoc :veryunlikely 100)
   (swap! (:future app-state) dissoc :veryunlikely)
  )
