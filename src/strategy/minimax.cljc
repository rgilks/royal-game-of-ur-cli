(ns strategy.minimax
  (:require [config]
            [engine]
            [platform]
            [util :refer [debug]]
            [view]))

(defn- score-player [game player]
  (let [off-board (get-in game [:players player :off-board])
        on-board-pieces (engine/get-piece-positions (:board game) player)
        on-board-count (count on-board-pieces)
        rosette-count (count (filter #(contains? (get-in config/board [:rosettes]) %) on-board-pieces))
        last-square (last (get-in config/board [:paths player]))
        pieces-ready-to-exit (count (filter #(= % last-square) on-board-pieces))
        score (+ (* 100 off-board)
                 (* 10 on-board-count)
                 (* 5 rosette-count)
                 (* 50 pieces-ready-to-exit))]
    (debug "Score" player score
           "[" on-board-count
           rosette-count
           pieces-ready-to-exit
           off-board  "]")
    score))

(defn- evaluate-state [game]
  (let [current-player (:current-player game)
        opponent (engine/other-player current-player)
        current-score (score-player game current-player)
        opponent-score (score-player game opponent)
        evaluation (- current-score opponent-score)]
    (debug "State eval:" current-player evaluation)
    evaluation))

(defn- order-moves [game moves]
  (let [ordered-moves (sort-by (fn [move]
                                 (let [next-state (engine/choose-action game move)]
                                   (- (score-player next-state (:current-player game)))))
                               moves)]
    ;; (debug "Ordered moves:" (map :to ordered-moves))
    (when (:debug @config/game)
      (view/show-moves ordered-moves))
    ordered-moves))

(defn- adaptive-depth [game base-depth]
  (let [pieces-left (+ (get-in game [:players :A :in-hand])
                       (get-in game [:players :B :in-hand]))
        depth (max 1 (min base-depth (+ base-depth (- 14 pieces-left))))]
    (debug (str "Adaptive depth: " depth " (base: " base-depth ", pieces left: " pieces-left ")"))
    depth))

(defn- safe-get-moves [game]
  (if (= (:state game) :choose-action)
    (engine/get-moves game)
    []))

(defn- minimax [game depth maximizing? alpha beta]
  (if (or (zero? depth) (= :end-game (:state game)))
    (let [value (evaluate-state game)]
      (debug "Leaf node reached. Value:" value)
      [value nil])
    (let [moves (order-moves game (safe-get-moves game))
          init-score (if maximizing? (- platform/infinity) platform/infinity)
          comparator (if maximizing? > <)]
      (if (empty? moves)
        (let [value (evaluate-state game)]
          (debug "No moves available. Value:" value)
          [value nil])
        (loop [idx 1
               [move & rest-moves] moves
               best-score init-score
               best-move nil
               alpha alpha
               beta beta]
          (if-not move
            (do
              (debug "Finished evaluating all moves at depth" depth ". Best score:" best-score)
              [best-score best-move])
            (let [_ (debug "Eval " idx "depth" depth)
                  [score _] (minimax (engine/choose-action game move)
                                     (dec depth)
                                     (not maximizing?)
                                     alpha
                                     beta)
                  [new-best-score new-best-move] (if (comparator score best-score)
                                                   [score move]
                                                   [best-score best-move])
                  new-alpha (if maximizing? (max alpha new-best-score) alpha)
                  new-beta (if-not maximizing? (min beta new-best-score) beta)]
              (debug "Move" move "evaluated. Score:" score)
              (if (<= beta alpha)
                (do
                  (debug "Pruning at depth" depth)
                  [new-best-score new-best-move])
                (recur (inc idx) rest-moves new-best-score new-best-move new-alpha new-beta)))))))))

(defn select-move [game]
  (debug "\nSelecting move for" (:current-player game))
  (when (seq (engine/get-possible-moves game))
    (let [base-depth (get-in game [:strategy :params :depth] 3)
          depth (adaptive-depth game base-depth)
          [score best-move] (minimax game depth true (- platform/infinity) platform/infinity)]
      (debug "Selected move:" best-move "with score" score)
      best-move)))

(defmethod engine/select-move :minimax [_ game]
  (select-move game))
