(ns durak.errors)

(def card-not-found {:code 11
                     :message "Card not found"})

(def cannot-defend {:code 12
                    :message "Cannot defend"})

(def not-attacking {:code 13
                    :message "Nobody is attacking"})

(def already-attacking {:code 14
                        :message "Already attacking"})

(def cannot-throw-in {:code 15
                      :message "Cannot throw in"})

(def wrong-player {:code 16
                   :message "Turn not allowed"})

(def unknown-command {:code 21
                      :message "Unknown command"})

(def not-websocket-request {:code 51
                            :message "Not websocket request"})

(def unknown-error {:code 91
                    :message "Unknown error"})

(defn make-exception [error]
  (ex-info (:message error) error))

(defn throw-error [error]
  (throw (make-exception error)))
