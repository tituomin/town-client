(ns town-client.map
  (:require
   [town-client.util :refer [log log-v]]
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
        el (sel1 (str ".map-ranking-content#" id " " ".g-mapcanvas"))
        map (google.maps.Map. el map-opts)]
    (swap! maps assoc id map)
    map))

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

(defn browser-is-ie
  []
  (= (.-appName js/navigator) "Microsoft Internet Explorer"))

(defn add-data
  [data]
  (if (:key data)
    (let 
        [key (:key data)
         map (or (@maps key) (add-map! (name key)))]
      (doseq [obj (vals (data :results))]
        (.addGeoJson (.-data map) obj)
        (.setStyle (.-data map)
                   (fn [feature]
                     (let [text (.getProperty feature "text_content")
                           category (.getProperty feature "category")
                           marker #js{:title (if (> (count text) 0) text category)}]
                       (if (not (browser-is-ie))
                         (set! (.-icon marker) #js{:path google.maps.SymbolPath.CIRCLE
                                               :scale 3
                                               :fillColor (color category)
                                               :strokeWeight 0
                                               :fillOpacity 1}))
                       marker)))))))

(defn init
  []
  (doseq [id ["asuminen"]]
    (add-map! id)))
