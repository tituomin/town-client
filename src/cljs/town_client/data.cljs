(ns town-client.data
  (:require
   [goog.net.XhrIo]
   [town-client.util :refer [log]]
   [town-client.config :as config]
   [cemerick.url :refer [url url-encode]]
   [cljs.core.async :as async
    :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))


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

(defn fetch-data
  [channel type params & path-elements]
  (.send goog.net.XhrIo 
         (-> (apply url (reduce conj [config/url-base type] path-elements))
             (assoc :query (assoc params :page_size (or (:page_size params) 100)))
             str)
         (feed-channel channel type path-elements)))
