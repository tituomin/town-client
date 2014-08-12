(ns town-client.ajax
  (:require
   [goog.net.XhrIo]
   [goog.net.XhrIoPool]
   [goog.net.EventType]
   [goog.events]
   [cljs.core.async :as async
    :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(def default-headers
  #js{"Accept" "application/json"})

(def pool
  (goog.net.XhrIoPool. default-headers))

(defn handle-response [success failure event]
  (let [xhr (.-currentTarget event)]
   (if (.isSuccess xhr)
     (async/put! success (.getResponseJson xhr))
     (async/put! failure {:status (.getStatus xhr)}))
   (.releaseObject pool xhr)))

(defn make-request [url]
  (let [success (chan)
        failure (chan)
        send (fn [xhrio]
               (.listen
                goog.events xhrio
                goog.net.EventType/COMPLETE
                (partial handle-response success failure))
               (.setTimeoutInterval xhrio 0)
               (.send xhrio url "GET"))]
    (.getObject pool send)
    [success failure]))

(comment
  ; REPL interaction
  (defn parse [response]
    (swap! result assoc :value (.getResponseJson (.-target response)))
    (swap! result assoc :error (.getStatus (.-target response)))
    (swap! result assoc :orig response))
  (defn handle-channels
    [{:keys [success failure]}]
    (go (while true
          (let [[value c] (alts! [failure success])]
            (println value)))))
  (get-data "respondents" {:neighborhood 370} nil parse)
  (get-data "rrgoijespondents" {:neighborhood 370} nil parse)
  (make-request "http://www.google.fi/")
  (make-request "http://www.telkku.com/")
  (make-request "http://dev.hel.fi/kenenkaupunki/api/respondents/?neighborhood=370")
  (make-request "http://dev.hel.fi/kenenkaupunki/api/responoijdents/?neighborhood=370")
  (make-request "http://www.woiegoimew.we/")
  (handle-channels (make-request "http://dev.hel.fi/kenenkaupunki/api/respondents/?neighborhood=370"))
  (handle-channels (make-request "http://dev.hel.fi/kenenkaupunki/api/respooijndents/?neighborhood=370"))

  )

