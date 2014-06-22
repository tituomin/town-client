(ns town-client.server
  (:require [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [hiccup.core :refer (html)]
            [hiccup.page :refer (html5)]
            [compojure.route :refer (resources not-found)]
            [compojure.core :refer (GET defroutes)]
            ring.adapter.jetty))

(defn page-html []
  (html5
   [:head [:script {:src "http://fb.me/react-0.9.0.js"}]
          [:script {:src "/js/town.js"}]]
   [:body
    [:div {:id "content-wrap"}]
    [:script (browser-connected-repl-js)]]))

(defroutes site
  (GET "/" req (page-html))
  (resources "/")
  (not-found "Page not found"))

(defn clojurescript []
  (def repl-env (reset! cemerick.austin.repls/browser-repl-env
                        (cemerick.austin/repl-env)))
  (cemerick.austin.repls/cljs-repl repl-env))

(defn run []
  (defonce ^:private server
    (ring.adapter.jetty/run-jetty #'site {:port 8080 :join? false}))
  server)

