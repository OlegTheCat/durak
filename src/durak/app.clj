(ns durak.app
  (:require [org.httpkit.server :as http-server]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [chord.http-kit :refer [wrap-websocket-handler]]
            [ring.middleware.resource :as resource]
            [durak.runner :as runner]
            [durak.logic :as logic]
            [durak.errors :as errors]
            [cheshire.core :as json]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [ring.util.response :refer [resource-response]])
  (:gen-class))

(def pending-channels (a/chan 10))

(defn render-game-state-for-player [player game-state]
  (-> game-state
      (update :discard-pile (comp count flatten))
      (update :deck count)
      (dissoc :hands)
      (dissoc :current-player)
      (assoc :hand (get-in game-state [:hands player]))
      (assoc :opponent-hand (-> game-state
                                (get-in [:hands (logic/opposite-player game-state player)])
                                count))
      (assoc :turn (= player (logic/current-player game-state)))
      (assoc :winner (if (= "draw" (:winner game-state))
                       "draw"
                       (= (:winner game-state) player)))))

(defn start-game [chan-1 chan-2]
  (let [player-1-id (str (java.util.UUID/randomUUID))
        player-2-id (str (java.util.UUID/randomUUID))
        initial-game-state (logic/make-game player-1-id player-2-id)]
    (log/infof "Started game for players: %s and %s" player-1-id player-2-id)
    (a/go-loop [game-state initial-game-state]
      (let [game-state-1-json (render-game-state-for-player player-1-id game-state)
            game-state-2-json (render-game-state-for-player player-2-id game-state)]
        (a/go (a/>! chan-1 game-state-1-json))
        (a/go (a/>! chan-2 game-state-2-json)))
      (when-not (:winner game-state)
        (let [[value ch] (a/alts! [chan-1 chan-2])
              message (:message value)]
          (if (nil? message)
            (log/warn "Player has been disconnected")
            (let [{:keys [result error]}
                  (runner/handle-command-safe (assoc message
                                                     :player (if (= ch chan-1)
                                                               player-1-id
                                                               player-2-id))
                                              game-state)]
              (if (some? error)
                (do
                  (a/>! ch {:error error})
                  (recur game-state))
                (recur result)))))))))

(defn pending-watcher [pending-channels]
  (a/go-loop [chan-1 (a/<! pending-channels)
              chan-2 (a/<! pending-channels)]
    (start-game chan-1 chan-2)
    (recur (a/<! pending-channels)
           (a/<! pending-channels))))

(defn start-pending-watcher []
  (pending-watcher pending-channels))

(defn ws-handler [{:keys [ws-channel] :as req}]
  (log/info "Received ws request")
  (a/go (a/>! pending-channels ws-channel)))

(defroutes app-routes
  (GET "/game" [] (wrap-websocket-handler
                   ws-handler
                   {:format :json-kw}))
  (route/not-found "Page not found"))

(defn -main []
  (start-pending-watcher)
  (log/info "Web server has been started")
  (http-server/run-server (-> #'app-routes
                              (resource/wrap-resource "public"))
                          {:ip "127.0.0.1"
                           :port 2444}))
