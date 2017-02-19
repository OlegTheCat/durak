(ns durak.client.dev
  (:require [durak.client.core :as core]))

(defn remount []
  (.renderDurak js/window))
