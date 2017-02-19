(ns durak.runner
  (:require [durak.logic :as logic]
            [durak.errors :as errors]
            [taoensso.timbre :as log]))

(defmulti handle-command (fn [command game-state]
                           (:name command)))

(defmethod handle-command "put-card" [{:keys [card
                                              player]} game-state]
  (cond
    (logic/no-attack? player game-state)
    (logic/attack player card game-state)

    (logic/attack-in-progress? player game-state)
    (errors/throw-error errors/wrong-player)

    (logic/attack-pending? player game-state)
    (logic/throw-in player card game-state)

    (logic/defense-pending? player game-state)
    (logic/defend player card game-state)

    :else
    (errors/throw-error errors/wrong-player)))

(defmethod handle-command "abandon-defense" [{:keys [player]} game-state]
  (logic/abandon-defense player game-state))

(defmethod handle-command "finish-attack" [{:keys [player]} game-state]
  (logic/finish-attack player game-state))

(defmethod handle-command :default [command _]
  (log/warn "Unknown command: " command)
  (errors/throw-error errors/unknown-command))


(defn handle-command-safe [command game-state]
  (try
    {:result (handle-command command game-state)}
    (catch clojure.lang.ExceptionInfo e
      {:error (ex-data e)})
    (catch Exception e
      (log/error e)
      {:error errors/unknown-error})))
