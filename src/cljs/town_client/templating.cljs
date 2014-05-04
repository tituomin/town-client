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

  ;; (log "apply-template")
  ;; (log-v params)
  ;; (log dom-el)

(defn apply-template
  ([dom-el params]
  (apply-template dom-el params (dm/attr dom-el :data-template)))

  ([dom-el params template-id]
  (let [template (.-innerHTML (sel1 (str "#" template-id)))
        parent (first (rest (dm/ancestor-nodes dom-el)))]
    (dm/set-html!
     parent
     (render template params)))))


(defn neighborhood-options [neighborhoods]
  (map (fn [n]
         (node [:option {:value (:id n)} (:name n)]))
       neighborhoods))
       
(defn choose-neighborhood [id]
  (aset (.-location js/window) "hash" id))

(defn populate-neighborhood-menu [neighborhoods]
  (let [dropdown (sel1 :.navigate-areas)
        options  (neighborhood-options neighborhoods)]
    (dm/clear! dropdown)
    (doseq [no options]
      (dm/append! dropdown no))
    (dm/listen! dropdown "change"
                #(choose-neighborhood (-> % .-target .-value)))))

(defn output-neighborhood [neighborhood neighborhoods]
  (log "output-neighborhood")
  (let [id (neighborhood "id")
        genetive-form (language/citizen-genetive ((neighborhood "name") "fi"))]
    (dm/set-text! (sel1 :.header-area) genetive-form)
    (dm/set-text! (sel1 "title") (str genetive-form " kaupunki 2015"))
    (doseq [key [:prev :next]]
      (set! (.-href (sel1 (str ".header-link-" (name key) " a")))
            (str "#" (key (neighborhoods id)))))))

(defn template-key [k]
  (if (or (not k) (number? k))
    (str "age_percent_" (or k "0"))
    (to-snake k)))

(defn output-stats [respondent-count answers]
  (log "output-stats")
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
      (log "here")
      (log key)
      (apply-template
       (sel1 (str "#" (name key) ".map-ranking-content .g-rankingarea div"))
       {:rankings
        (apply str (for [x list] (str "<li class=\"rankingarea\">" (:name x) " " (:count x) "</li>")))}))))

(defn add-event-handlers [channel]
  (dm/listen!
   js/window "hashchange"
   #(async/put!
     channel 
     { :type :newlocation,
       :location
      (apply str 
             (-> % .-currentTarget
                 .-location .-hash rest))})))

(defn reinit-page
  [user-channel]
  (apply-template (sel1 ".page div") {} "kaupunginosa")
  ;; (doseq [placeholder (sel "div[data-placeholder=\"true\"]")]
  ;;     (apply-template placeholder {}))

  )

(defn init [channel]
  (log "Init templating.")
  (add-event-handlers channel)
  (apply-template (sel1 ".site-header div") {} "site-header")
)
