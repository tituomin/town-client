(ns town-client.repl
  (:require [town-client.config :as config :refer [aggregates]]
            [town-client.control :as control]
            [town-client.data :as data]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

#_(
  (cemerick.austin.repls/exec)
  )
