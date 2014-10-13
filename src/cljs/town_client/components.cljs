
(ns town-client.components
  (:require
   [town-client.util :refer [log-v log]]
   [town-client.state :as state :refer [app-state]]
   [town-client.language :as language]
   [town-client.intents :as intents]
   [cljs.core.async :as async]
   [clojure.string]
   [kioo.core]
   [clojure.browser.repl]
   [kioo.reagent :refer [content set-attr set-class add-class remove-class do-> substitute listen append]]
   [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]
                   [kioo.reagent :refer [defsnippet deftemplate]]
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

(defsnippet autocomplete-menu-item "public/index.html"
  [[:.neighborhood-autocomplete-menu-item first-of-type]]
  [user-channel neighborhood index]
  {[root] (do->
           (content (:name neighborhood))
           (set-attr :key (:id neighborhood))
           (set-attr :href (str "#" (:id neighborhood)))
           (if (= index (@state/search :selected))
             (add-class "selected")
             (remove-class "selected"))
           ;; (listen :onMouseOut
           ;;         #(async/put! user-channel (intents/highlight-neighborhood
           ;;                                    nil :menu)))
           (listen :onMouseOver
                   #(async/put! user-channel (intents/highlight-neighborhood
                                              (:id neighborhood) :menu))))})

(defn select-menu-item [channel item]
  (async/put! channel (intents/highlight-neighborhood (:id item) :query)))

(defsnippet autocomplete-menu "public/index.html"
  [[:.g-info-section :.neighborhood-input] :.neighborhood-autocomplete-menu]
  [user-channel index input]
  {[root] (let [results (@state/search :results)
                results-count (count results)]
              (do->
               (set-attr :style {:display (if (< 1 results-count) :block :none)})
               (set-attr :data-count (count results))
               (content (map (partial autocomplete-menu-item user-channel)
                             results (range))))
              )})

(defsnippet neighborhood-name "public/index.html"
  [[:.g-info-section :.neighborhood-map] :#neighborhood-name]
  [current-neighborhood mouse-cursor]
  {[root] (condp = (:source @current-neighborhood)
            :map
            (do->
             (set-attr :style {:display :block :left (+ 20 (or (:x @mouse-cursor) 0)) :top (- (or (:y @mouse-cursor) 0) 5)})
             (content (:name @current-neighborhood))
             )
            (set-attr :style {:display :none}))
            })

(defsnippet neighborhood-search "public/index.html"
  [[:.g-info-section :.neighborhood-input] :#neighborhood-text-input]
  [search-state user-channel]
  {[root] (let [text-value (@search-state :input)
         input-val (if (= 1 (count (@search-state :results)))
                     (:name (first (@search-state :results)))
                     (or text-value ""))
         prefix-length (count text-value)]
     (do->
      (set-attr :value input-val)
      (if (nil? text-value) (remove-class "has-text")
          (add-class "has-text"))
      (listen :on-change #(state/search-neighborhoods (.-value (.-target %)) user-channel))
      (listen :on-focus #(swap! search-state assoc :input ""))
      (listen :on-render (fn [component]
                            (let [in-el (.getDOMNode component)]
                             (.setSelectionRange in-el prefix-length (count input-val)))))
      (listen :on-key-down
              #(condp = (.-which %)
                 ;backspace
                 8 (do
                     (state/search-neighborhoods (subs text-value
                            0 (- (count text-value) 1))
                     (.preventDefault %) user-channel))
                 13 (when (or
                           (= 1 (count (@search-state :results)))
                           (not (nil? (:id @state/current-neighborhood))))
                      (async/put! user-channel
                                  (intents/neighborhood-intent
                                   (:id @state/current-neighborhood))))
                 40 (state/select-next user-channel)
                 38 (state/select-prev user-channel)
                 nil))
      #_(listen :on-blur #(reset! state/search-input nil))))})

(deftemplate landing-page "public/index.html"
  [user-channel]
  {[:header.site-header :.site-navigation] (substitute "")
   [:#neighborhood-map] (do->
                         (content (town-map nil))
                         (set-attr :style {:height "600px" :width "100%"}))
   [[:.g-info-section :.neighborhood-map] :#neighborhood-name]
   (substitute (neighborhood-name state/current-neighborhood state/mouse-cursor))
   [[:.g-info-section :.neighborhood-input] :#neighborhood-text-input]
   (substitute (neighborhood-search state/search user-channel))
   [[:.g-info-section :.neighborhood-input] :.neighborhood-autocomplete-menu]
   (substitute (autocomplete-menu user-channel (@state/search :index) (@state/search :input)))})

(defn init []
  (reagent/render-component [neighborhood-page]
                            (.getElementById js/document "content-wrap")))

(defn init-front [user-channel]
  (fn []
      (reagent/render-component [head "Kenen kaupunki"]
                                (.item (.getElementsByTagName js/document "head") 0))
      (reagent/render-component [landing-page user-channel]
                                (.getElementById js/document "content-wrap"))      )
    )

; ----- REPL testing -----

#_(
   :component-did-update
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
