(ns durak.client.controller
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [durak.client.shared :refer [raise! config-ref]]
            [durak.client.controllers.board :as board]))


(def controllers {:board board/control})

(defn- broadcast [msg args world]
  (reduce #(%2 msg args %1) world (vals controllers)))

(defmulti control
  (fn [msg args world]
    (if (vector? msg) ::redirect msg)))

(defmethod control :default [msg args world]
  (println "Warning! Unhandled message:" msg)
  world)

(defmethod control ::redirect [[c msg] args world]
  ((controllers c) msg args world))

(defmethod control :init [msg args world]
  (broadcast msg args world))

(defmethod control :mount [_ args world]
  (raise! :load)
  world)

(defmethod control :load [msg args world]
  (broadcast msg args world))
