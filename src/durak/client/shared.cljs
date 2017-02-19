(ns durak.client.shared
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan <! >!]]))

(def default-configuration
  {:env "dev"
   :ws-host "ws://127.0.0.1:2444/game"})

(defonce config-ref (atom default-configuration))

(defn config [key]
  (if (vector? key)
    (get-in @config-ref key)
    (get @config-ref key)))

(defn configure [config-source]
  (let [config-params (js->clj config-source
                               :keywordize-keys true)]
    (swap! config-ref into config-params)))

(defonce message-chan (chan))

(defn raise! [& args]
  (go (>! message-chan args)))

(defn raiser [controller]
  (fn [msg & args]
    (let [message
          (if (vector? msg) msg
              [controller msg])]
      (apply raise! message args))))
