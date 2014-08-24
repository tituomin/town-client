(ns town-client.map
  (:require
   [town-client.util :refer [log log-v]]
   [town-client.state :as state]
   [town-client.config :as config]
   [cemerick.url :refer [url url-encode]])
  (:use-macros
   [dommy.macros :only [sel sel1]]))

(def lat 60.168398)
(def lon 24.939811)

(def maps (atom {}))

(defn add-map!
  [id]
  (let [map-opts #js{"center" (google.maps.LatLng. lat lon)
                     "zoom" 11
                     "panControl" false
                     "zoomControl" true
                     "mapTypeControl" false
                     "scaleControl" false
                     "streetViewControl" false
                     "overviewMapControl" false }
        selector (str ".mapview#" id " " ".g-maparea")
        el (sel1 selector)
        map (google.maps.Map. el map-opts)
        ]
    (swap! maps assoc id map)
    map))

(defn choose-neighborhood [id]
  (aset (.-location js/window) "hash" id))

(defn add-neighborhood-map!
  [id]
  (let [helsinki-center (google.maps.LatLng. 60.1143400903318 25.0171297094567)
        helsinki-bounds (google.maps.LatLngBounds.
                         (google.maps.LatLng. (+ 0.202 59.9224) (+ 24.7828 0.1))
                         (google.maps.LatLng. (- 60.2978 0.05) (- 25.2544 0.1)))
        map-opts #js{"center" helsinki-center
                     "panControl" false
                     "zoom" 10
                     "draggable" false
                     "scrollwheel" false
                     "zoomControl" false
                     "mapTypeControl" false
                     "scaleControl" false
                     "streetViewControl" false
                     "overviewMapControl" false
                     "styles" config/front-page-map-styles
                     }
        current-feature (atom {})
        selector (str "#" id)
        el (sel1 selector)
        get-geometry (fn [nhood]
                       (-> nhood :geometry (get "coordinates")))
        skatta (-> (@state/neighborhoods 384) :geometry (get "coordinates"))
        gmap (google.maps.Map. el map-opts)
        features (map (fn [[key nhood]]
                        (google.maps.Data.Feature.
                         #js{:geometry
                             (google.maps.Data.MultiPolygon.
                              (state/vector-to-array
                               (state/coords-to-google
                                (get-geometry nhood))))
                             :id key
                             :properties
                             #js{"name" (:name nhood)
                                 }}
        )) @state/neighborhoods)
        ]
    (.log js/console config/front-page-map-styles)
    (doseq [feature features]
      (.add (.-data gmap) feature))
    (.addListener js/google.maps.event (.-data gmap) "click"
                  #(choose-neighborhood (.getId (.-feature %))))
    (.setStyle (.-data gmap) #js{:fillColor "#FF0000" :strokeWeight 2})
;    (.setCenter gmap helsinki-center (.getBoundsZoomLevel helsinki-bounds))
;    (.panToBounds gmap helsinki-bounds)    
    (.fitBounds gmap helsinki-bounds)
;; map_center = bounds.getCenter();
;; map.setCenter(map_center);
;; map.panToBounds(bounds);
;; map.fitBounds(bounds);
;; google.maps.event.addListenerOnce(yourMap, 'bounds_changed', function(event) {
;;   if (this.getZoom() > 15) {
;;     this.setZoom(15);
;;   }
;; });
    ;; (.addListenerOnce js/google.maps.event gmap "bounds_changed" #(do (.setZoom gmap 11)
    ;;                                              (.log js/console %)))
    (.addListener
     js/google.maps.event
     (.-data gmap) "mousemove"
     #(swap! current-feature assoc :position (.-latLng %))
     )
    (.addListener js/google.maps.event (.-data gmap) "mouseover"
                  (fn [ob]
                    (let [feature (.-feature ob)
                          name (.getProperty feature "name")
                          position (.-latLng ob)
                          feature-id (.getId feature)
                          old-feature-id (:id current-feature)
                          ]
                      (swap! current-feature assoc :id feature-id :name name)
                      (.overrideStyle (.-data gmap) feature #js{:fillColor "#00FF00"})
                      (.log js/console (:name @current-feature))
                      (.addListenerOnce js/google.maps.event (.-data gmap) "mouseout"
                                        #(do
                                           (reset! current-feature {})
                                           (.revertStyle (.-data gmap) feature))
                                        ))))
    gmap))

(defn icon
  [color]
  (str "http://maps.google.com/mapfiles/ms/icons/" color ".png"))

(defn color
  [key]
  (condp = key
   ;"#ef889d"

    "paikka-tai-alue-asuinrakentamiselle" "#363636"
    "alue-ei-ole-virkistykselle-valttamaton-paikalle-voisi-rakentaa" "#708f38"

    "paikka-toimistoille-palveluille-tai-liiketiloille" "#363636"
    "taalla-pitaisi-olla-enemman-kauppoja-ja-palveluita-rakennusten-kivijaloissa" "#990000"

    "virkistyksellisesti-tarkea-mutta-saisi-olla-laadultaan-parempi" "#000099"
    "taalla-on-tallaisenaan-ainutlaatuista-kaupunkiluontoa" "#363636"

    "taalla-ymparisto-nayttaa-ankealta-ja-sita-pitaisi-parantaa-esimerkiksi-puuistutuksin" "#e2591b"
    "huonosti-hoidettu-epamaarainen-alue-jota-tulisi-parantaa" "#363636"

    "black"
    ))

(defn browser-is-ie []
  (= (.-appName js/navigator) "Microsoft Internet Explorer"))

(defn add-data
  [data]
  (if (:key data)
    (let 
        [key (:key data)
         map (add-map! (name key))]
      (doseq [obj (vals (data :results))]
        (.addGeoJson (.-data map) obj)
        (.setStyle (.-data map)
                   (fn [feature]
                     (let [text (.getProperty feature "text_content")
                           category (.getProperty feature "category")
                           marker #js{:title (if (> (count text) 0) text category)}]
                       (if (not (browser-is-ie))
                         (set! (.-icon marker)
                               #js{:path google.maps.SymbolPath.CIRCLE
                                   :scale 3
                                   :fillColor (color category)
                                   :strokeWeight 0
                                   :fillOpacity 1}))
                       marker)))))))

(defn init
  []
  (doseq [id ["asuminen"]]
    (add-map! id)))
