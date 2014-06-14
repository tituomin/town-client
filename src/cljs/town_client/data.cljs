(ns town-client.data
  (:require
   [goog.net.XhrIo]
   [goog.net.XhrIoPool]
   [town-client.util :refer [log]]
   [town-client.config :as config]
   [cemerick.url :refer [url url-encode map->query]]
   [cljs.core.async :as async
    :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn parse-response
  []
  nil)

(def pool
  (goog.net.XhrIoPool. #js{"Accept" "application/json"}))

(defn get-xhrio [callback]
  (.getObject pool))

(defn feed-channel [channel type path]
  (fn [reply-value]
    (let [response (.-target reply-value)
          object   (.getResponseJson response)
          message  {:resource-type (keyword type)}
          results (cond
                   (aget object "results") (.-results object)
                   (array? object) object
                   true nil)]
      (put! channel
            (if results
              (assoc message :results
                     (map (fn [o] (js->clj o {:keywordize-keys true}))
                          (js->clj results)))
              (if (= type "answers")
                object ; google maps excepts an object
                (merge message
                       (js->clj object {:keywordize-keys true}))))))))

(defn api-url [type params path-elements]
  (let [defaults    {:page_size 100}
        real-params (merge defaults params)
        url-string  (str config/url-base type "/"
                        (if (not-empty path-elements)
                          (clojure.string/join "/" path-elements))
                        (if (not-empty real-params) "?"))
        url         (str url-string (map->query real-params))]
    (print (str "Fetching " url "\n"))
    url))

(defn get-data [type params path-elements callback]
  (.send goog.net.XhrIo
         (api-url type params path-elements)
         callback "GET" nil
         ))

(defn fetch-data [channel type params & path-elements]
  (get-data type params path-elements
   (feed-channel channel type path-elements)))

(comment
  ; REPL interaction
  (def result (atom {:value nil :error nil :orig nil}))
  (defn jkeys [o] (.keys js/Object o))
  (defn parse [response]
    (swap! result assoc :value (.getResponseJson (.-target response)))
    (swap! result assoc :error (.getStatus (.-target response)))
    (swap! result assoc :orig response))
  (get-data "respondents" {:neighborhood 370} nil parse)
  (get-data "rrgoijespondents" {:neighborhood 370} nil parse)
  )
