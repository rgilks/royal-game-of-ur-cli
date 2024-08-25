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
    #_(debug "Score" player score
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
    ;; (debug "State eval:" current-player evaluation)
    evaluation))

(defn- order-moves [game moves]
  (sort-by (fn [move]
             (let [next-state (engine/choose-action game move)]
               (- (score-player next-state (:current-player game)))))
           moves))

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

(def dice-probabilities
  {0 1/16  ; (1/2)^4
   1 1/4   ; 4 * (1/2)^3 * (1/2)
   2 3/8   ; 6 * (1/2)^2 * (1/2)^2
   3 1/4   ; 4 * (1/2) * (1/2)^3
   4 1/16}) ; (1/2)^4

(def avg-prob
  (/ (reduce + (vals dice-probabilities))
     (count dice-probabilities)))

(defn dampened-prob
  [roll dampening]
  (+ (* (dice-probabilities roll) (- 1 dampening))
     (* avg-prob dampening)))

(defn- simulate-roll [game roll]
  (-> game
      (assoc :roll roll)
      (assoc :state :choose-action)))

(def minimax-cache (atom {}))

(defn- cache-key [game depth maximizing? damp]
  [(:board game) (:current-player game) depth damp maximizing?])

(defn- minimax [game depth maximizing? alpha beta damp]
  (let [cache-k (cache-key game depth maximizing? damp)
        indent (apply str (repeat (- 3 depth) "  "))
        player-symbol (if maximizing? "↑" "↓")
        infinity-symbol "∞"
        alpha-str (if (= alpha (- platform/infinity)) (str "-" infinity-symbol) (platform/fmt-num "%.2f" alpha))
        beta-str (if (= beta platform/infinity) infinity-symbol (platform/fmt-num "%.2f" beta))]
    (debug (str indent player-symbol " D" depth " α:" alpha-str " β:" beta-str))
    (if-let [cached-result (get @minimax-cache cache-k)]
      (do
        (debug (str indent "↺ " (platform/fmt-num "%.2f" (first cached-result))))
        cached-result)
      (if (or (zero? depth) (= :end-game (:state game)))
        (let [value (evaluate-state game)]
          (debug (str indent "⚑ " (platform/fmt-num "%.2f" value)))
          [value nil])
        (let [moves (order-moves game (safe-get-moves game))
              init-score (if maximizing? (- platform/infinity) platform/infinity)
              comparator (if maximizing? > <)]
          (if (empty? moves)
            (let [roll-scores (for [roll (range 5)]
                                (let [rolled-game (simulate-roll game roll)
                                      [score _] (minimax rolled-game (dec depth) (not maximizing?) alpha beta damp)]
                                  (* (dampened-prob roll damp) score)))
                  avg-score (/ (apply + roll-scores) (count roll-scores))]
              (debug (str indent "∅ " (platform/fmt-num "%.2f" avg-score)))
              [avg-score nil])
            (loop [remaining-moves moves
                   best-score init-score
                   best-move nil
                   alpha alpha
                   beta beta]
              (if (empty? remaining-moves)
                (do
                  (debug (str indent "★ Best: " (view/format-move best-move) " (" (platform/fmt-num "%.2f" best-score) ")"))
                  (let [result [best-score best-move]]
                    (swap! minimax-cache assoc cache-k result)
                    result))
                (let [move (first remaining-moves)
                      roll-scores (for [roll (range 5)]
                                    (let [next-game (-> game
                                                        (engine/choose-action move)
                                                        (simulate-roll roll))
                                          [score _] (minimax next-game (dec depth) (not maximizing?) alpha beta damp)]
                                      (* (dampened-prob roll damp) score)))
                      avg-score (/ (apply + roll-scores) (count roll-scores))
                      [new-best-score new-best-move] (if (comparator avg-score best-score)
                                                       [avg-score move]
                                                       [best-score best-move])
                      new-alpha (if maximizing? (max alpha new-best-score) alpha)
                      new-beta (if-not maximizing? (min beta new-best-score) beta)]
                  (debug (str indent "→ " (view/format-move move) " (" (platform/fmt-num "%.2f" avg-score) ")"))
                  (if (<= beta alpha)
                    (do
                      (debug (str indent "✂ Pruned"))
                      (let [result [new-best-score new-best-move]]
                        (swap! minimax-cache assoc cache-k result)
                        result))
                    (recur (rest remaining-moves) new-best-score new-best-move new-alpha new-beta)))))))))))

(defn select-move [game]
  (debug "\nSelecting move for" (:current-player game))
  (when (seq (engine/get-possible-moves game))
    (let [base-depth (get-in game [:strategy :params :depth] 3)
          depth (adaptive-depth game base-depth)
          damp (get-in game [:strategy :params :damp] 0.5)
          [score best-move] (minimax game depth true (- platform/infinity) platform/infinity damp)]
      (debug ">" (view/format-move best-move) score)
      best-move)))

(defmethod engine/select-move :minimax [_ game]
  (select-move game))
