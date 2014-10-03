(ns town-client.main
 (:require
  [cljs.core.async :as async]
  [town-client.state]
  [town-client.components :as components]
  [town-client.control :as control]))

(let [user-channel (async/chan)]
  (set! (.-onload js/window) (components/init-front user-channel))
  (control/init user-channel))

;(components/init-front)

