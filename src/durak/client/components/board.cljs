(ns durak.client.components.board
  (:require [durak.client.shared :refer [raiser]]
            [rum.core :as rum]))

(def raise! (raiser :board))

(rum/defc Deck [count]
  [:div "Cards left in deck: " count])

(rum/defc Card
  ([card]
   (Card nil card))
  ([on-click {:keys [rank suit]}]
   [:div
    {:onClick on-click}
    (str rank " of " suit)]))

(defn- card-key [{:keys [rank suit]}]
  (str rank suit))

(rum/defc PlayerHand [cards]
  [:div
   [:span "Your cards"]
   [:div
    (map (fn [card]
           (rum/with-key
             (Card #(raise! :put-card card) card)
             (card-key card))) cards)]])

(rum/defc OpponentHand [count]
  [:div (str "Opponent has " count " cards")])

(rum/defc DiscardPile [count]
  [:div (str count " cards in discard pile")])

(rum/defc Turn [turn]
  (if turn
    [:div "Your turn"]
    [:div "Opponent's turn"]))

(rum/defc Board [{:keys [defended attacking]}]
  [:div
   "Board"
   [:div
    (when (some? attacking)
      [:span "Attacking card is: " (Card attacking)])

    [:br]

    (when (seq defended)
      [:div
       "Defended cards"
       (map
        (fn [[attacked defended]]
          [:div {:key (str (card-key attacked)
                           (card-key defended))}
           [:span (Card attacked) " <--> " (Card defended)]])
        defended)])

    [:br]

    (when (and (not (some? attacking))
               (not (seq defended)))
      [:div "Nothing there yet"])]])

(rum/defc Actions []
  [:div
   [:div
    {:onClick #(raise! :abandon-defense)}
    "Abandon defense"]
   [:div
    {:onClick #(raise! :finish-attack)}
    "Finish attack"]])

(rum/defc Trump [trump]
  [:div "Trump is: " trump])

(rum/defc Winner [winner]
  (when winner
    (if (= winner "draw")
      [:div "It's a draw..."]
      [:div "You won!!!"])))

(rum/defc Table [{:keys [game-state]}]
  (let [{:keys [discard-pile
                trump
                deck
                hand
                board
                opponent-hand
                turn
                winner]} game-state]
    (println game-state)
    [:div
     (OpponentHand opponent-hand)
     [:br]
     (Deck deck)
     [:br]
     (Board board)
     [:br]
     (PlayerHand hand)
     [:br]
     (OpponentHand opponent-hand)
     [:br]
     (DiscardPile discard-pile)
     [:br]
     (Trump trump)
     [:br]
     (Turn turn)

     [:br][:br]
     (Actions)
     [:br][:br]
     (Winner winner)]))
