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

(def key-icon
  {:verylikely    "fa fa-thumbs-o-up"
   :quitelikely   "fa fa-smile-o"
   :notsure       "fa fa-meh-o"
   :quiteunlikely "fa fa-frown-o"
   :veryunlikely  "fa fa-thumbs-o-down"
   :single        "maki-lodging"
   :couple        "maki-toilet"
   :withkids      "maki-school"
   :group         "maki-theatre"
   :other         "maki-town-hall"
   :car           "maki-fuel"
   :bike          "maki-bicycle"
   :walk          "maki-pitch"
   :public        "maki-bus"
   })

(defn max-icon [data]
  (if (== 0 (count data))
      "maki-prison"
      (key-icon (key (apply max-key val data)))))

(defsnippet neighborhood-menu
  "public/kaupunginosa/index.html"
  [:select.navigate-areas]
  [neighborhoods]
  {[Root] (content (map neighborhood-menu-item neighborhoods))})

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
       {:future-accommodation (atom {}) :family-situation (atom {})
        :transport-preferences (atom {}) :age (atom {})})

(defsnippet background-info-section
  "public/kaupunginosa/index.html"
  [[:.g-info-section :.background]] []
  {[:.choice-graphs [:.g-choice :.future]]
   (substitute (future-visualisation (:future-accommodation app-state)))
   [:.choice-graphs [:.g-choice :.family]]
   (substitute (family-visualisation (:family-situation app-state)))
   [:.choice-graphs [:.g-choice :.transport]]
   (substitute (transport-visualisation (:transport-preferences app-state)))
   [:.choice-graphs [:.g-choice :.age]]
   (substitute (age-visualisation (:age app-state)))
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
   (reset! (:future-accommodation app-state) {:verylikely 90
         :quitelikely 100
         :notsure 30
         :quiteunlikely 40
         :veryunlikely 20})
   (reset! (:transport-preferences app-state) {:car 10
                                               :bike 20
                                               :walk 30
                                               :public 40})
   (swap! (:future-accommodation app-state) assoc :veryunlikely 90)
   (swap! (:future-accommodation app-state) dissoc :veryunlikely)
  )
