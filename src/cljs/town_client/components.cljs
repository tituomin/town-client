(ns town-client.components
  (:require
   [town-client.util :refer [log-v log]]
   [town-client.state :as state :refer [app-state]]
   [town-client.language :as language]
   [clojure.string]
   [kioo.core]
   [clojure.browser.repl]
   [kioo.reagent :refer [content set-attr do-> substitute listen append]]
   [reagent.core :as reagent :refer [atom]])
  (:require-macros [kioo.reagent :refer [defsnippet deftemplate]]
                   [town-client.macros :refer [defstatvisualisation]]))

(def template-path "public/kaupunginosa/index.html")
(def mock-neighborhood-genetive "Kalliolaisten")

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
   :not-found     "maki-prison"
   })

(defn max-icon [data]
  (let [top-key
        (if (== 0 (count data))
          :not-found
          (key (apply max-key val data)))]
    (or (key-icon top-key)
        (key-icon :not-found))))

(defsnippet neighborhood-menu-item
  "public/kaupunginosa/index.html"
  [:select.navigate-areas [:option first-of-type]]
  [{:keys [name id]}]
  {[root] (do-> (content name)
                (set-attr :value id)
                (set-attr :key id)
                )})

(defn choose-neighborhood [id]
  (aset (.-location js/window) "hash" id))

(defsnippet neighborhood-menu
  "public/kaupunginosa/index.html"
  [:select.navigate-areas]
  [neighborhoods]
  {[root]
   (do->
    (content (map neighborhood-menu-item
                  (sort-by :name (vals @neighborhoods))))
    (listen :on-change #(choose-neighborhood (-> % .-target .-value)))
    )})

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

(defsnippet background-info-section
  "public/kaupunginosa/index.html"
  [[:.g-info-section :.background]] []
  {[:.choice-graphs [:.g-choice :.future]]
   (substitute (future-visualisation
                (:future-accommodation app-state)))
   [:.choice-graphs [:.g-choice :.family]]
   (substitute (family-visualisation
                (:family-situation app-state)))
   [:.choice-graphs [:.g-choice :.transport]]
   (substitute (transport-visualisation
                (:transport-preferences app-state)))
   [:.choice-graphs [:.g-choice :.age]]
   (substitute (age-visualisation
                (:age app-state)))
   })

(defsnippet head "public/kaupunginosa/index.html" [:head] [neighborhood]
  {[:title] (content neighborhood)})

(defsnippet neighborhood-header "public/kaupunginosa/index.html"
  [:.page-header]
  [neighborhood]
  {[:.header-area] (content (:genetive neighborhood))
   [:.header-link-prev :a] (set-attr :href (str "#" (:prev neighborhood)))
   [:.header-link-next :a] (set-attr :href (str "#" (:next neighborhood)))})

(deftemplate page "public/kaupunginosa/index.html" []
  {[:head]
   (substitute (head))
   [:.navigate-areas]
   (substitute (neighborhood-menu state/neighborhoods))
   [:.page-header]
   (substitute (neighborhood-header @state/current-neighborhood))
   [[:.g-info-section :.background]]
   (substitute (background-info-section))
})

(defn init [channel]
  (reagent/render-component [head "Kenen kaupunki"]
                            (.item (.getElementsByTagName js/document "head") 0))
  (reagent/render-component [page]
                            (.getElementById js/document "content-wrap"))
  )

#_(
   (reset! state/neighborhoods {1 {:name "foo" :id 1 :prev nil :next nil}})
   (reset! (:future-accommodation app-state) {:verylikely 0
         :quitelikely 0
         :notsure 0
         :quiteunlikely 100
         :veryunlikely 0})
   (reset! (:transport-preferences app-state) {:car 10
                                               :bike 20
                                               :walk 30
                                               :public 40})
   (swap! (:future-accommodation app-state) assoc :veryunlikely 90)
   (swap! (:future-accommodation app-state) dissoc :veryunlikely)
   (swap! (:age app-state) assoc :16 50)
   (swap! (:family-situation app-state) assoc :withkids 70)
  )
