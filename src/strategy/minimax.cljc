(ns strategy.minimax
  (:require [engine :as state]
            [platform]))

(defn- score-player [game player]
  (+ (* 10 (get-in game [:players player :off-board]))
     (count (engine/get-piece-positions (:board game) player))))

(defn- evaluate-state [game]
  (let [current-player (:current-player game)
        opponent (engine/other-player current-player)]
    (- (score-player game current-player)
       (score-player game opponent))))

(defn- get-next-state [game move]
  (-> game
      (engine/choose-action move)
      (assoc :roll (reduce + (repeatedly 4 #(rand-int 2))))))

(defn- safe-get-moves [game]
  (if (= (:state game) :choose-action)
    (engine/get-moves game)
    []))

(defn- minimax [game depth maximizing? alpha beta]
  (if (or (zero? depth) (= :end-game (:state game)))
    [(evaluate-state game) nil]
    (let [moves (safe-get-moves game)
          init-score (if maximizing? (- platform/infinity) platform/infinity)
          comparator (if maximizing? > <)]
      (if (empty? moves)
        [(evaluate-state game) nil]
        (loop [[move & rest-moves] moves
               best-score init-score
               best-move nil
               alpha alpha
               beta beta]
          (if-not move
            [best-score best-move]
            (let [[score _] (minimax (get-next-state game move)
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

(defn select-move [game]
  (when (seq (engine/get-possible-moves game))
    (let [depth (get-in game [:strategy :params :depth] 3)]  ; Default to depth 3 if not specified
      (second (minimax game depth true (- platform/infinity) platform/infinity)))))

(defmethod engine/select-move :minimax [_ game]
  (select-move game))
