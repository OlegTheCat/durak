(ns durak.client.core
  (:require [rum.core :as rum]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close!]]

            [durak.client.shared :as shared]
            [durak.client.world :as world]
            [durak.client.components.board :refer [Table]]
            [durak.client.components.common :refer [Wrapper]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defonce lifecycle-chan (chan))

(defn- lifecycle-raise! [& args]
  (go (>! lifecycle-chan args)))

(defn ^:export main-component []
  (Wrapper lifecycle-raise! #(Table @world/world-ref)))

(defn ^:export configure [config-source]
  (shared/configure config-source))

(defn- lifecycle-loop []
  (go
    (while true
      (let [[msg react-cmp] (<! lifecycle-chan)]
        (case msg
          :will-mount
          (do
            (add-watch world/world-ref :force-update #(rum/request-render react-cmp))
            (shared/raise! :mount))
          :will-unmount
          (remove-watch world/world-ref :force-update)
          nil)))))

(defonce start
  (do
    (lifecycle-loop)
    (shared/raise! :init)))
