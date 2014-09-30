(ns town-client.server
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.edn :as edn]
   [cemerick.austin]
   [cemerick.austin.repls]
   [hiccup.core :refer (html)]
   [hiccup.page :refer (html5)]
   [compojure.route :refer (resources not-found)]
   [cljs.closure :as cljsc]
   [clojure.java.shell :refer (sh with-sh-dir)]
   [clojure.java.browse :refer (browse-url)]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [cemerick.austin]
   [cemerick.austin.repls :as repls :refer [browser-repl-env browser-connected-repl-js exec]]
   [compojure.core :refer (GET ANY POST defroutes)]
   [compojure.route :as route]
   [compojure.handler :refer [site]]
   [ring.util.response :as res]
   [ring.adapter.jetty :refer (run-jetty)]))

; todo: automatically check optimization level (-> "/path/to/project.clj" slurp read-string (nth 2))

(defn page-html []
  (html5
   [:head
    [:script {:src "js/goog/base.js"}] ; none
    [:script {:src "http://fb.me/react-0.9.0.js"}]
    [:script {:src "js/town.js"}] ; none
    [:script "goog.require('town_client.components');"] ; none
    [:script "goog.require('town_client.main');"] ; none
    ]
   [:body
    [:div {:id "content-wrap"}]
    [:script {:type "text/javascript" :src "https://maps.googleapis.com/maps/api/js?key=AIzaSyCsfgos0fa9QSD47DTl7N540KrcU1Pgwyk&amp;sensor=false"}]
    [:script (browser-connected-repl-js)]
]))

(defroutes app
  (GET "/" req (page-html))
  (resources "/")
  (not-found "Page not found"))


(defn headless []
  (cemerick.piggieback/cljs-repl :repl-env (cemerick.austin/exec-env)))

; Above: headless repl
; Below: browser base environment and repl

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root
   #'system
   (fn [system]
     (if-not (:server system)
       {:server (run-jetty #'app {:port 3000 :join? false})
        :repl-env (reset! browser-repl-env (cemerick.austin/repl-env))}
       (do (.start (:server system)) system)))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (cemerick.austin.repls/cljs-repl (:repl-env system) :optimizations :none))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (when (try (.stop (:server system))
             (catch Throwable e false))
    true))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh-all :after 'town-client.server/go))

;; (ns town-client.server
;;   (:require [cemerick.austin.repls :refer (browser-connected-repl-js)]
;;             ring.adapter.jetty))




;; (defn run []
;;   (defonce ^:private server
;;     (ring.adapter.jetty/run-jetty #'site {:port 8080 :join? false}))
;;   server)


;; TODO INCORPORATE
