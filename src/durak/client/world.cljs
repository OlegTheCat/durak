(ns durak.client.world
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer (<!)]
            [durak.client.shared :refer [message-chan config]]
            [durak.client.controller :refer [control]]))

(def initial-world
  {})

(defonce world-ref (atom initial-world))

(defonce start
  (go
    (while true
      (let [[msg & args] (<! message-chan)]
        (when (= (config :env) "dev")
          (console.log "--> raise!:" msg args))
        (swap! world-ref #(control msg args %))))))
