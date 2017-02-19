(ns durak.logic
  (:require [durak.errors :as errors]))


(def ^:private ranks [6 7 8 9 10 "jack" "queen" "king" "ace"])
(def ^:private suits ["spades" "hearts" "diamonds" "clubs"])

(defn- gen-deck []
  (for [rank ranks
        suit suits]
    {:rank rank
     :suit suit}))

(def ^:private deck (take 16 (gen-deck)))

(defn- higher-card-by-rank? [card-1 card-2]
  (> (.indexOf ranks (:rank card-1))
     (.indexOf ranks (:rank card-2))))

(defn- equal-cards-rank? [card-1 card-2]
  (= (:rank card-1)
     (:rank card-2)))

(defn- lower-card-by-rank? [card-1 card-2]
  (and (not (higher-card-by-rank? card-1 card-2))
       (not (equal-cards-rank? card-1 card-2))))

(defn- trump-card? [trump {:keys [suit]}]
  (= trump suit))

(defn- detect-starting-player [trump hands]
  (let [[[player-1 player-1-hand]
         [player-2 player-2-hand]] (seq hands)
        player-1-trumps (set (filter (partial trump-card? trump) player-1-hand))
        player-2-trumps (set (filter (partial trump-card? trump) player-2-hand))]
    (if-let [trumps (seq (concat player-1-trumps
                                 player-2-trumps))]
      (if (contains? player-1-hand (first (sort-by :rank trumps)))
        player-1 player-2)
      ;; just choose random player if players don't have trumps
      (first (shuffle [player-1 player-2])))))

(defn make-game [player-1 player-2]
  (let [shuffled-deck (shuffle deck)
        [player-cards rest-deck] (split-at 12 shuffled-deck)
        [player-1-cards player-2-cards] (split-at 6 player-cards)
        hands {player-1 (set player-1-cards)
               player-2 (set player-2-cards)}
        trump-card (first rest-deck)
        trump (:suit trump-card)
        final-deck (-> rest-deck
                       rest
                       vec
                       (conj trump-card))]
    {:current-player (detect-starting-player trump hands)
     :trump trump
     :deck final-deck
     :hands hands
     :board {:defended []
             :attacking nil}
     :discard-pile []
     :winner nil}))

(defn current-player [game-state]
  (:current-player game-state))

(defn opposite-player [game-state player]
  (let [[[player-1 _]
         [player-2 _]] (seq (:hands game-state))]
    (if (= player-1 player)
      player-2
      player-1)))

(defn- not-current-player [game-state]
  (opposite-player game-state (current-player game-state)))

(defn- current-player-hand [{:keys [hands]
                             :as game-state}]
  (get hands (current-player game-state)))

(defn- not-current-player-hand [{:keys [hands]
                                 :as game-state}]
  (get hands (not-current-player game-state)))

(defn- attacking [game-state]
  (get-in game-state [:board :attacking]))

(defn- defended [game-state]
  (get-in game-state [:board :defended]))

(defn- assert-has-card [hand card]
  (when-not (contains? hand card)
    (errors/throw-error errors/card-not-found)))

(defn- assert-attacking [game-state]
  (when-not (some? (attacking game-state))
    (errors/throw-error errors/not-attacking)))

(defn attack-pending? [player game-state]
  (and (= (current-player game-state) player)
       (not (some? (attacking game-state)))
       (seq (defended game-state))))

(defn attack-in-progress? [player game-state]
  (and (= (current-player game-state) player)
       (some? (attacking game-state))))

(defn no-attack? [player game-state]
  (and (= (current-player game-state) player)
       (not (some? (attacking game-state)))
       (not (seq (defended game-state)))))

(defn defense-pending? [player game-state]
  (and (= (not-current-player game-state) player)
       (some? (attacking game-state))))

(defn- assert-current-player [player game-state]
  (when-not (= player (current-player game-state))
    (errors/throw-error errors/wrong-player)))

(defn- assert-not-current-player [player game-state]
  (when-not (= player (not-current-player game-state))
    (errors/throw-error errors/wrong-player)))

(defn attack [player card game-state]
  (assert-current-player player game-state)
  (let [hand (current-player-hand game-state)]
    (assert-has-card hand card)
    (-> game-state
        (assoc-in [:board :attacking] card)
        (update-in [:hands (current-player game-state)] disj card))))

(defn- higher-card-by-rank-and-trump? [trump card-1 card-2]
  (if (= (:suit card-1) (:suit card-2))
    (higher-card-by-rank? card-1 card-2)
    (trump-card? trump card-1)))

(defn defend [player card {:keys [trump]
                           :as game-state}]
  (assert-not-current-player player game-state)
  (let [hand (not-current-player-hand game-state)]
    (assert-has-card hand card)
    (assert-attacking game-state)

    (when-not (higher-card-by-rank-and-trump?
               (:trump game-state)
               card
               (attacking game-state))
      (errors/throw-error errors/cannot-defend))

    (-> game-state
        (assoc-in [:board :attacking] nil)
        (update-in [:hands (not-current-player game-state)] disj card)
        (update-in [:board :defended] conj [(attacking game-state) card]))))

(defn- can-throw-in? [defended card]
  (->> defended
       flatten
       (filter #(equal-cards-rank? % card))
       first
       some?))

(defn throw-in [player card game-state]
  (assert-current-player player game-state)
  (let [hand (current-player-hand game-state)]
    (assert-has-card hand card)
    (when (some? (attacking game-state))
      (errors/throw-error errors/already-attacking))
    (when (and (not (some? (attacking game-state)))
               (not (some? (defended game-state))))
      (errors/throw-error errors/not-attacking))

    (when-not (can-throw-in? (defended game-state) card)
      (errors/throw-error errors/cannot-throw-in))


    ;; TODO: handle 'too many cards on board' case
    (-> game-state
        (assoc-in [:board :attacking] card)
        (update-in [:hands (current-player game-state)] disj card))))

(defn- hand-full? [hand]
  (>= (count hand) 6))

(defn- hands-full? [hands]
  (let [[[_ hand-1]
         [_ hand-2]] (seq hands)]
    (and (hand-full? hand-1)
         (hand-full? hand-2))))

(defn- distribute-cards [game-state]
  (loop [current-deck (:deck game-state)
         draw-player (current-player game-state)
         current-hands (:hands game-state)]
    (cond
      (not (seq current-deck))
      [current-hands []]

      (hands-full? current-hands)
      [current-hands current-deck]

      (hand-full? (get-in game-state [:hands draw-player]))
      (recur current-deck (opposite-player game-state draw-player) current-hands)

      :else
      (recur (rest current-deck)
             (opposite-player game-state draw-player)
             (update current-hands draw-player conj (first current-deck))))))

(defn draw-cards [game-state]
  (let [[new-hands new-deck] (distribute-cards game-state)]
    (-> game-state
        (assoc :deck new-deck)
        (assoc :hands new-hands))))

(defn- set-winner-opt [game-state]
  (if-not (seq (:deck game-state))
    (let [current-player-empty? (not (seq (current-player-hand game-state)))
          not-current-player-empty? (not (seq (not-current-player-hand game-state)))]
      (cond
        (and current-player-empty? not-current-player-empty?)
        (assoc game-state :winner "draw")

        current-player-empty?
        (assoc game-state :winner (current-player game-state))

        not-current-player-empty?
        (assoc game-state :winner (not-current-player game-state))

        :else
        game-state))
    game-state))

(defn abandon-defense [player game-state]
  (assert-not-current-player player game-state)
  (assert-attacking game-state)
  (-> game-state
      (assoc-in [:board :attacking] nil)
      (assoc-in [:board :defended] [])
      (update-in [:hands (not-current-player game-state)]
                 conj (attacking game-state))
      (update-in [:hands (not-current-player game-state)]
                 into (flatten (defended game-state)))
      set-winner-opt
      draw-cards))

(defn finish-attack [player game-state]
  (assert-current-player player game-state)
  (when (some? (attacking game-state))
    (errors/throw-error errors/already-attacking))
  (when (and (not (some? (attacking game-state)))
             (not (some? (defended game-state))))
    (errors/throw-error errors/not-attacking))

  (-> game-state
      (assoc-in [:board :attacking] nil)
      (assoc-in [:board :defended] [])
      (update :discard-pile into (defended game-state))
      set-winner-opt
      draw-cards
      (assoc :current-player (not-current-player game-state))))
