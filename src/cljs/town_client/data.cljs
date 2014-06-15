(ns town-client.data
  (:require
   [goog.net.XhrIo]
   [goog.net.XhrIoPool]
   [town-client.util :refer [log]]
   [town-client.ajax :as ajax]
   [town-client.config :as config]
   [cemerick.url :refer [url url-encode map->query]]
   [clojure.string :refer [join]]
   [cljs.core.async :as async
    :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn to-clj [value]
  (map #(js->clj % {:keywordize-keys true}) value))

(defn data-value [object type]
  (let [res-type (keyword type)
        value (if (array? object)
                (js->clj object)
                [object])]
    (assoc {:resource-type res-type}
      :results
      (if (= res-type :answers)
        object ; Map answers are delivered as-is to Google Maps.
        (to-clj value)))))

(defn api-url [type params path-elements]
  (let [defaults {:page_size 100}
        all-params (merge defaults params)
        path (concat [config/url-base type] path-elements)]
    (str (join "/" path)
         (if-not (seq path-elements) "/")
         (if (seq all-params) "?")
         (map->query all-params))))

(defn fetch-data [data-channel type params & path-elements]
  (go
    (let [channels (ajax/make-request
                    (api-url type params path-elements))
          [success failure] channels
          [value channel] (alts! channels)]
      (condp = channel
        success (put! data-channel (data-value value type))
        failure (log (join " "
                           ["Error fetching"
                            type params path-elements
                            "status" (:status value)]))))))

#_(
  ; REPL interaction
  (def result (atom {:value nil :error nil :orig nil}))
  (defn jkeys [o] (.keys js/Object o))
  (defn parse [response]
    (swap! result assoc :value (.getResponseJson (.-target response)))
    (swap! result assoc :error (.getStatus (.-target response)))
    (swap! result assoc :orig response))
  (get-data "respondents" {:neighborhood 370} nil parse)
  (get-data "rrgoijespondents" {:neighborhood 370} nil parse)
  (ajax/make-request (api-url "respondents" {:neighborhood 370} nil))
  )
