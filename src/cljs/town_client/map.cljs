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
  (log "adding map ")
  (log id)
  (let [map-opts (clj->js {"center" (google.maps.LatLng. lat lon)
                           "zoom" 11})
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
    "paikka-tai-alue-asuinrakentamiselle" "blue"
    "alue-ei-ole-virkistykselle-valttamaton-paikalle-voisi-rakentaa" "red"
    "green"
    ))

(defn add-data
  [data]
  (if (:key data)
    (let 
        [key (:key data)
         map (or (@maps key) (add-map! (name key)))]
      (log "adding data")
      (log key)
      (doseq [obj (vals (data :results))]
        (.addGeoJson (.-data map) obj)
        (.setStyle (.-data map)
                   (fn [feature]
                     (let [text (.getProperty feature "text_content")
                           category (.getProperty feature "category")]
                       (clj->js {:title (if (> (count text) 0) text category)
                                 :icon (icon (color category))}))))))))

(defn init
  []
  (doseq [id ["asuminen"]]
    (add-map! id)))
