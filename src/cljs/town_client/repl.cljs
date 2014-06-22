(ns town-client.repl
  (:require [town-client.config :as config :refer [aggregates]]
            [town-client.control :as control]
            [town-client.data :as data]
            [clojure.browser.repl]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))


#_(
   ;(cemerick.austin.repls/exec)
   (def repl-env (reset! cemerick.austin.repls/browser-repl-env (cemerick.austin/repl-env)))
   (cemerick.austin.repls/cljs-repl repl-env)
   (let [[data-channel user-channel incomplete-channel] (repeat chan)])
   )
