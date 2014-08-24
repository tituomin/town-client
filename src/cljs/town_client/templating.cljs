(ns town-client.templating
  (:require
   [town-client.util :refer [log log-v make-js-object]]
   [town-client.language :as language]
   [dommy.core :as dm]
   [cljs.core.async :as async])
  (:use-macros
   [dommy.macros :only [node sel sel1]]))

(defn render [template data]
  ((.template js/_ template)
   (make-js-object data)
   (set! (.-variable (js-obj)) "data")))

(defn to-snake [s]
  (clojure.string/replace (str (or s "ei-vastausta")) #"-" "_"))

(def stat-templates
  {
   "life_situation"              "family"
   "transport_mode_first"        "transport"
   "probability_stay_five_years" "future"
   "age_low"                     "age"
  })

(defn apply-template
  ([dom-el params]
  (apply-template dom-el params (dm/attr dom-el :data-template)))

  ([dom-el params template-id]
  (let [template (.-innerHTML (sel1 (str "#" template-id)))
        parent (first (rest (dm/ancestor-nodes dom-el)))]
    (dm/set-html!
     parent
     (render template params)))))


(defn neighborhood-options [neighborhoods selected]
  (map (fn [n]
         (node [:option
                ^:attrs (if (= (:id n) (js/parseInt selected))
                          {:value (:id n) :selected :selected}
                          {:value (:id n)})
                (:name n)]))
       neighborhoods))
       
;24.7828 59.9224 60.2978 25.2544
;24.783 59.923 60.298 25.255
; center of helsinki 25.0171297094567 60.1143400903318
(defn choose-neighborhood [id]
  (aset (.-location js/window) "hash" id))

(defn populate-neighborhood-menu [neighborhoods selected]
  (let [dropdown (sel1 :.navigate-areas)
        options  (neighborhood-options neighborhoods selected)]
    (dm/clear! dropdown)
    (doseq [no options]
      (dm/append! dropdown no))
    (dm/listen! dropdown "change"
                #(choose-neighborhood (-> % .-target .-value)))))

(defn output-neighborhood [neighborhood neighborhoods]
  (let [id (neighborhood "id")
        genetive-form (language/citizen-genetive ((neighborhood "name") "fi"))]
    (dm/set-text! (sel1 :.header-area) genetive-form)
    (dm/set-text! (sel1 "title") (str genetive-form " kaupunki 2050"))
    (doseq [key [:prev :next]]
      (set! (.-href (sel1 (str ".header-link-" (name key) " a")))
            (str "#" (key (neighborhoods id)))))))

(defn template-key [k]
  (if (or (not k) (number? k))
    (str "age_percent_" (or k "0"))
    (to-snake k)))

(defn output-stats [respondent-count answers]
  (doseq [[key values] answers]
    (let [templatekey (stat-templates key)
          placeholder (sel1 (str "div[data-template=\""
                                 (str templatekey "-spread") "\"]"))]
      (apply-template
       placeholder
       (into {} (map
                 (fn [[k v]]
                   [(template-key k)
                    (* 100 (/ v respondent-count))])
                 (answers key)))))))

(defn output-map-stats
  [stats key]
  (if key
    (let [list (take 5 (sort-by (fn [x]
                                  (- (:count x))) stats))]
      (apply-template
       (sel1 (str "#" (name key) ".map-ranking-content .g-rankingarea div"))
       {:rankings
        (apply str (for [x list] (str "<li class=\"rankingarea\">" (:name x) " " (:count x) "</li>")))}))))

(defn neighborhood-intent [id]
  {:type :newlocation, :location id })

(defn add-event-handlers [channel]
  (dm/listen!
   js/window "hashchange"
   #(async/put!
     channel
     (neighborhood-intent
      (subs
       (-> % .-currentTarget
           .-location .-hash) 1)))))

(defn reinit-page
  [user-channel]
  (apply-template (sel1 ".page div") {} "kaupunginosa"))

(defn init [channel]
  (add-event-handlers channel)
  (apply-template (sel1 ".site-header div") {} "site-header"))
