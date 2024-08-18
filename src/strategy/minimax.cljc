(ns strategy.minimax
  (:require [platform]
            [state :as state]))

(def max-depth 3)

(defn- score-player [game-state player]
  (+ (* 10 (get-in game-state [:players player :off-board]))
     (count (state/get-piece-positions (:board game-state) player))))

(defn- evaluate-state [game-state]
  (let [current-player (:current-player game-state)
        opponent (state/other-player current-player)]
    (- (score-player game-state current-player)
       (score-player game-state opponent))))

(defn- get-next-state [game-state move]
  (-> game-state
      (state/choose-action move)
      (assoc :roll (reduce + (repeatedly 4 #(rand-int 2))))))

(defn- safe-get-moves [game-state]
  (if (= (:state game-state) :choose-action)
    (state/get-moves game-state)
    []))

(defn- minimax [game-state depth maximizing? alpha beta]
  (if (or (zero? depth) (= :end-game (:state game-state)))
    [(evaluate-state game-state) nil]
    (let [moves (safe-get-moves game-state)
          init-score (if maximizing? (- platform/infinity) platform/infinity)
          comparator (if maximizing? > <)]
      (if (empty? moves)
        [(evaluate-state game-state) nil]
        (loop [[move & rest-moves] moves
               best-score init-score
               best-move nil
               alpha alpha
               beta beta]
          (if-not move
            [best-score best-move]
            (let [[score _] (minimax (get-next-state game-state move)
                                     (dec depth)
                                     (not maximizing?)
                                     alpha
                                     beta)
                  [new-best-score new-best-move] (if (comparator score best-score)
                                                   [score move]
                                                   [best-score best-move])
                  new-alpha (if maximizing? (max alpha new-best-score) alpha)
                  new-beta (if-not maximizing? (min beta new-best-score) beta)]
              (if (<= beta alpha)
                [new-best-score new-best-move]
                (recur rest-moves new-best-score new-best-move new-alpha new-beta)))))))))

(defn select-move [possible-moves game-state]
  (when (seq possible-moves)
    (second (minimax game-state max-depth true (- platform/infinity) platform/infinity))))

(defmethod state/select-move :minimax [_ possible-moves game-state]
  (select-move possible-moves game-state))