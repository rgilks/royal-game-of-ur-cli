(ns strategy.minimax
  (:require [config]
            [engine :as engine]
            [platform]))

(def transposition-table (atom {}))

(defn- get-cached-evaluation [game]
  (get @transposition-table (hash game)))

(defn- cache-evaluation [game value]
  (swap! transposition-table assoc (hash game) value))

(defn- score-player [game player]
  (let [off-board (get-in game [:players player :off-board])
        on-board-pieces (engine/get-piece-positions (:board game) player)
        on-board-count (count on-board-pieces)
        rosette-count (count (filter #(contains? (get-in config/board [:rosettes]) %) on-board-pieces))
        last-square (last (get-in config/board [:paths player]))
        pieces-ready-to-exit (count (filter #(= % last-square) on-board-pieces))]
    (+ (* 100 off-board)
       (* 10 on-board-count)
       (* 5 rosette-count)
       (* 50 pieces-ready-to-exit))))

(defn- evaluate-state [game]
  (let [current-player (:current-player game)
        opponent (engine/other-player current-player)]
    (- (score-player game current-player)
       (score-player game opponent))))

(defn- order-moves [game moves]
  (sort-by (fn [move]
             (let [next-state (engine/choose-action game move)]
               (- (score-player next-state (:current-player game)))))
           moves))

(defn- adaptive-depth [game base-depth]
  (let [pieces-left (+ (get-in game [:players :A :in-hand])
                       (get-in game [:players :B :in-hand]))]
    (max 1 (min base-depth (+ base-depth (- 14 pieces-left))))))

(defn- safe-get-moves [game]
  (if (= (:state game) :choose-action)
    (engine/get-moves game)
    []))

(defn- minimax [game depth maximizing? alpha beta]
  (if-let [cached-value (get-cached-evaluation game)]
    [cached-value nil]
    (if (or (zero? depth) (= :end-game (:state game)))
      (let [value (evaluate-state game)]
        (cache-evaluation game value)
        [value nil])
      (let [moves (order-moves game (safe-get-moves game))
            init-score (if maximizing? (- platform/infinity) platform/infinity)
            comparator (if maximizing? > <)]
        (if (empty? moves)
          (let [value (evaluate-state game)]
            (cache-evaluation game value)
            [value nil])
          (loop [[move & rest-moves] moves
                 best-score init-score
                 best-move nil
                 alpha alpha
                 beta beta]
            (if-not move
              (do
                (cache-evaluation game best-score)
                [best-score best-move])
              (let [[score _] (minimax (engine/choose-action game move)
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
                  (do
                    (cache-evaluation game new-best-score)
                    [new-best-score new-best-move])
                  (recur rest-moves new-best-score new-best-move new-alpha new-beta))))))))))

(defn select-move [game]
  (when (seq (engine/get-possible-moves game))
    (let [base-depth (get-in game [:strategy :params :depth] 3)
          depth (adaptive-depth game base-depth)]
      (second (minimax game depth true (- platform/infinity) platform/infinity)))))

(defmethod engine/select-move :minimax [_ game]
  (select-move game))
