(ns town-client.main
 (:require
  [town-client.state]
  [town-client.components :as components]
  [town-client.control :as control]))

(set! (.-onload js/window) components/init)
(control/init)
