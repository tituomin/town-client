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

(def template-path "public/kaupunginosa.html")
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

(def age-ranges
  [[:0 :15] [:16 :19] [:20 :24]
  [:25 :29] [:30 :39] [:40 :49]
  [:50 :59] [:60 :69] [:70 :?]])

(def age-strings
  (into
   {}
   (map (fn [[k v]]
          [k (str (name k) "-" (name v))])
        age-ranges)))

(defn top-choice [data]
  (if (== 0 (count data))
          :not-found
          (key (apply max-key val data))))

(defn max-class [data]
  (str "choice-color-" (name (top-choice data))))

(defn max-icon [data]
    (or (key-icon (top-choice data))
        (key-icon :not-found)))

(defn max-value [data]
  (age-strings (top-choice data)))

(defsnippet neighborhood-menu-item
  "public/kaupunginosa.html"
  [:select.navigate-areas [:option first-of-type]]
  [{:keys [name id]}]
  {[root] (do-> (content name)
                (set-attr :value id)
                (set-attr :key id)
                )})

(defn choose-neighborhood [id]
  (aset (.-location js/window) "hash" id))

(defsnippet neighborhood-menu
  "public/kaupunginosa.html"
  [:select.navigate-areas]
  [neighborhood neighborhoods]
  {[root]
   (do->
    (content (map neighborhood-menu-item
                  (sort-by :name (vals @neighborhoods))))
    (set-attr :value (:id @neighborhood))
    (listen :on-change #(choose-neighborhood (-> % .-target .-value)))
    )})

(defstatvisualisation future-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.future]]
  :.choice-column :.choice-line :.choice-value
  [:verylikely :quitelikely :notsure :quiteunlikely :veryunlikely])

(defstatvisualisation family-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.family]]
  :.choice-column :.choice-line :.choice-value
  [:single :couple :withkids :group :other])

(defstatvisualisation transport-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.transport]]
  :.choice-column :.choice-line :.choice-value
  [:car :bike :walk :public])

(defstatvisualisation age-visualisation
  town-client.config/master-template
  [:.choice-graphs [:.g-choice :.age]]
  :.choice-column :.choice-line :.choice-value
  [:0 :16 :20 :25 :30 :40 :50 :60 :70])

(defstatvisualisation opinion-visualisation
  town-client.config/master-template
  [:.g-value-questions :.value-questions]
  :.value-question :.value-question-graphic-spacer :.nonexistent
  [:agree_add_density
   :agree_add_my_area_density_for_less_cars
   :agree_bulevardisation
   :agree_high_rise
   :agree_suburbs_build_near_stations
   :enjoy_culture_urban_meetings
   :enjoy_metropolis_fascinating_possibilities
   :enjoy_outdoors_large_woods
   :my_area_could_be_built_more
   :prefer_daily_shopping_near
   :would_use_rail_transport_more
   ])

(defsnippet background-info-section
  "public/kaupunginosa.html"
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

(defsnippet head "public/kaupunginosa.html" [:head] [neighborhood]
  {[:title] (content neighborhood)})

(defsnippet map-ranking-item
  "public/kaupunginosa.html"
  [[:li :.rankingarea first-of-type]]
  [{:keys [name count]}]
  {[root] (content (str name " " count))})

(defsnippet info-map "public/kaupunginosa.html"
  [:.mapview]
  [ranking]
  {[root] (set-attr :id (first ranking))
   [:.g-rankingarea :.rankingarea-content :.rankingarea-list]
   (content (map map-ranking-item (second ranking)))
   [:.g-maparea] (do->
                  (content nil)
                  (set-attr :style {:height "300px"}))
   }
  )

(defsnippet neighborhood-header "public/kaupunginosa.html"
  [:.page-header]
  [neighborhood neighborhoods]
  {[:.header-area] (content (:genetive neighborhood))
   [:.header-link-prev :a] (set-attr :href (str "#" (:prev neighborhood)))
   [:.header-link-next :a] (set-attr :href (str "#" (:next neighborhood)))
   [:.header-link-destination--left] (content (:name (@neighborhoods (:prev neighborhood))))
   [:.header-link-destination--right] (content (:name (@neighborhoods (:next neighborhood))))
   [:.header-responders-count :.responders-count] (content (:respondent-count neighborhood))
   })


(deftemplate neighborhood-page "public/kaupunginosa.html" []
  {[:head]
   (substitute (head))
   [:.navigate-areas]
   (substitute (neighborhood-menu state/current-neighborhood state/neighborhoods))
   [:.page-header]
   (substitute (neighborhood-header @state/current-neighborhood state/neighborhoods))
   [[:.g-info-section :.background]]
   (substitute (background-info-section))
   [[:.g-info-section :.map]]
   (content (map info-map @state/rankings))
   [[:.g-info-section :.values] :.g-value-questions :.value-questions]
   (substitute (opinion-visualisation (:opinions app-state)))
})

(defsnippet town-map "public/kaupunginosa.html"
  [:#neighborhood-map]
  [neighborhoods]
  {[root] (content nil)})

(deftemplate landing-page "public/index.html"
  []
  {[:#neighborhood-map] (do->
                         (content (town-map nil))
                         (set-attr :style {:height "600px" :width "100%"}))
})

(defn init []
  (reagent/render-component [neighborhood-page]
                            (.getElementById js/document "content-wrap"))
  )

(defn init-front []
  (reagent/render-component [head "Kenen kaupunki"]
                             (.item (.getElementsByTagName js/document "head") 0))
  (reagent/render-component [landing-page]
                            (.getElementById js/document "content-wrap")))


; ----- REPL testing -----

#_(
   (def foo (atom
             {:agree_add_density 0
              :agree_add_my_area_density_for_less_cars 0
              :agree_bulevardisation 0
              :agree_high_rise 0
              :agree_suburbs_build_near_stations 0
              :enjoy_culture_urban_meetings 0
              :enjoy_metropolis_fascinating_possibilities 0
              :enjoy_outdoors_large_woods 0
              :my_area_could_be_built_more 0
              :prefer_daily_shopping_near 0
              :would_use_rail_transport_more 0
              }))
   (reset! state/neighborhoods {1 {:name "foo" :id 1 :prev nil :next nil}})
   (reset! (:future-accommodation app-state) {:verylikely 0
         :quitelikely 0
         :notsure 0
         :quiteunlikely 100
         :veryunlikely 0})
   (reset! (:transport-preferences app-state)
           {:car 10
            :bike 20
            :walk 30
            :public 40})
   (swap! (:future-accommodation app-state) assoc :veryunlikely 90)
   (swap! (:future-accommodation app-state) dissoc :veryunlikely)
   (swap! (:age app-state) assoc :16 50)
   (swap! (:family-situation app-state) assoc :withkids 70)
  )
