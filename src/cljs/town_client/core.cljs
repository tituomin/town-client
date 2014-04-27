(ns town-client.core
  (:require
   [dommy.core :as dm])
  (:use-macros
   [dommy.macros :only [node sel sel1]]))

(defn log [msg] (.log js/console msg))

(defn handle-click []
  (js/alert "Hello!"))

(defn init []
  (log "Initialising")
  ;; (def clickable (.getElementById js/document "clickable"))
  ;; (.addEventListener clickable "click" handle-click)
  )


(init)
