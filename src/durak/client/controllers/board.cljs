(ns durak.client.controllers.board
  (:require [durak.client.shared :refer [raiser config]]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def raise! (raiser :board))

(defmulti control (fn [msg args world] msg))

(defmethod control :init [_ _ world]
  world)

(defmethod control :load [_ _ world]
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch (config :ws-host)
                                                {:format :json-kw}))]
      (if-not error
        (do
          (println "Successfully initialized ws connection")
          (raise! :save-ws-channel ws-channel))
        (println "Couldn't init ws connection: " error))))
  world)

(defmethod control :save-ws-channel [_ [ws-channel] world]
  (raise! :start-ws-loop)
  (assoc world :ws-channel ws-channel))

(defmethod control :start-ws-loop [_ _ {:keys [ws-channel]
                                        :as world}]
  (go-loop []
    (let [val (<! ws-channel)]
      (if (nil? val)
        (js/alert "Disconnected from server")
        (do
          (raise! :handle-response val)
          (recur)))))
  world)

(defmethod control :handle-response [_ [{:keys [message]}] world]
  (println "---> received message: " message)
  (if-some [error (:error message)]
    (do (js/alert (:message error))
        world)
    (assoc world :game-state message)))

(defn- write-message [ws-channel message]
  (go (>! ws-channel message)))

(defmethod control :put-card [_ [card] {:keys [game-state]
                                        :as world}]
  (write-message (:ws-channel world) {:name "put-card"
                                      :card card})
  world)

(defmethod control :abandon-defense [_ _ world]
  (write-message (:ws-channel world) {:name "abandon-defense"})
  world)

(defmethod control :finish-attack [_ _ world]
  (write-message (:ws-channel world) {:name "finish-attack"})
  world)

(defmethod control :default [msg args world]
  (println "Warning! Unhandled message:" [:board msg])
  world)
