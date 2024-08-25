(ns strategy.mcts
  (:require [clojure.math :as math]
            [config]
            [engine]
            [platform])
  #?(:clj (:import [java.util.concurrent Executors Future])))

(defrecord Node [game move parent children visits value rave-value])

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
  (if-let [cached-value (get-cached-evaluation game)]
    cached-value
    (let [current-player (:current-player game)
          opponent (engine/other-player current-player)
          value (- (score-player game current-player)
                   (score-player game opponent))]
      (cache-evaluation game value)
      value)))

(defn- get-next-state [game move]
  (if (= :end-game (:state game))
    game
    (-> game
        (engine/choose-action move)
        (as-> g
              (if (= :roll-dice (:state g))
                (engine/roll g)
                g))
        (assoc :selected-move nil))))

(defn- expand [node]
  (if (= :end-game (:state (:game node)))
    node
    (let [moves (engine/get-possible-moves (:game node))
          new-children (mapv (fn [move]
                               (->Node (get-next-state (:game node) move)
                                       move
                                       node
                                       []
                                       0
                                       0.0
                                       0.0))
                             moves)]
      (assoc node :children new-children))))

(defn- select-best-child [node exploration-param rave-param]
  (when (and (seq (:children node)) (not= :end-game (:state (:game node))))
    (let [parent-visits (max 1 (:visits node))]
      (apply max-key
             (fn [child]
               (if (zero? (:visits child))
                 platform/infinity
                 (let [uct-score (+ (/ (:value child) (:visits child))
                                    (* exploration-param
                                       (math/sqrt (/ (math/log parent-visits)
                                                     (:visits child)))))
                       rave-score (/ (:rave-value child) (max 1 (:visits child)))
                       beta (/ (:visits child) (+ (:visits child) (* rave-param (:visits node))))
                       move-score (if (= (:to (:move child)) :off-board) 1000 0)]
                   (+ (* (- 1 beta) uct-score)
                      (* beta rave-score)
                      move-score))))
             (:children node)))))

(defn- backpropagate [node outcome moves]
  (loop [current node
         move-index 0]
    (when current
      (let [updated (-> current
                        (update :visits inc)
                        (update :value + outcome))]
        (when (< move-index (count moves))
          (let [move (nth moves move-index)]
            (if-let [child (first (filter #(= (:move %) move) (:children current)))]
              (-> child
                  (update :rave-value + outcome)
                  (update :visits inc)))))
        (if-let [parent (:parent current)]
          (recur (update parent :children
                         (fn [children]
                           (mapv #(if (= (:move %) (:move current)) updated %) children)))
                 (inc move-index))
          updated)))))

(defn- simulate [game]
  (loop [state game
         steps 0
         moves []]
    (if (or (> steps 100) (= :end-game (:state state)))
      [(evaluate-state state) moves]
      (case (:state state)
        :roll-dice (recur (engine/roll state) (inc steps) moves)
        :choose-action (let [possible-moves (engine/get-possible-moves state)]
                         (if (seq possible-moves)
                           (let [move (rand-nth possible-moves)]
                             (recur (get-next-state state move) (inc steps) (conj moves move)))
                           (recur (assoc state :state :roll-dice :selected-move nil) (inc steps) moves)))
        (recur (assoc state :state :roll-dice :selected-move nil) (inc steps) moves)))))

(defn- tree-policy [node exploration-param rave-param]
  (loop [current node]
    (if (= :end-game (:state (:game current)))
      current
      (if (empty? (:children current))
        (let [expanded (expand current)]
          (if (seq (:children expanded))
            (rand-nth (:children expanded))
            expanded))
        (if-let [best-child (select-best-child current exploration-param rave-param)]
          (if (zero? (:visits best-child))
            best-child
            (recur best-child))
          current)))))

#?(:clj
   (defn- parallel-mcts-search [root iterations exploration-param rave-param]
     (let [num-threads (.. Runtime getRuntime availableProcessors)
           iterations-per-thread (quot iterations num-threads)
           executor (Executors/newFixedThreadPool num-threads)
           tasks (repeatedly num-threads
                             #(fn []
                                (loop [current-root root
                                       iter iterations-per-thread]
                                  (if (zero? iter)
                                    current-root
                                    (let [node (tree-policy current-root exploration-param rave-param)
                                          [simulation-result moves] (simulate (:game node))
                                          updated-root (backpropagate node simulation-result moves)]
                                      (recur updated-root (dec iter)))))))]
       (try
         (let [futures (.invokeAll executor tasks)
               results (map #(.get ^Future %) futures)]
           (reduce (fn [acc result]
                     (update acc :children
                             (fn [children]
                               (mapv (fn [child result-child]
                                       (assoc child
                                              :visits (+ (:visits child) (:visits result-child))
                                              :value (+ (:value child) (:value result-child))
                                              :rave-value (+ (:rave-value child) (:rave-value result-child))))
                                     children (:children result)))))
                   root
                   results))
         (finally
           (.shutdown executor)))))

   :cljs
   (defn- parallel-mcts-search [root iterations exploration-param rave-param]
     (loop [current-root root
            iter iterations]
       (if (zero? iter)
         current-root
         (let [node (tree-policy current-root exploration-param rave-param)
               [simulation-result moves] (simulate (:game node))
               updated-root (backpropagate node simulation-result moves)]
           (recur updated-root (dec iter)))))))

(defn select-move [game]
  (let [possible-moves (engine/get-possible-moves game)]
    (when (seq possible-moves)
      (let [iterations (get-in game [:strategy :params :iterations] 10000)
            exploration-param (get-in game [:strategy :params :exploration] 1.41)
            rave-param (get-in game [:strategy :params :rave] 300)
            root (->Node game nil nil [] 0 0.0 0.0)
            expanded-root (expand root)
            best-node (parallel-mcts-search expanded-root iterations exploration-param rave-param)]
        (if-let [best-child (when (seq (:children best-node))
                              (apply max-key :visits (:children best-node)))]
          (:move best-child)
          (rand-nth possible-moves))))))

(defmethod engine/select-move :mcts [_ game]
  (select-move game))
