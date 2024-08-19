(ns strategy.mcts
  (:require [clojure.math :as math]
            [config]
            [game :as game]
            [platform]))

(defrecord Node [game move parent children visits value])

(defn- score-player [game player]
  (+ (* 10 (get-in game [:players player :off-board]))
     (count (game/get-piece-positions (:board game) player))))

(defn- evaluate-state [game]
  (let [current-player (:current-player game)
        opponent (game/other-player current-player)]
    (- (score-player game current-player)
       (score-player game opponent))))

(defn- get-next-state [game move]
  (if (= :end-game (:state game))
    game  ; If the game has ended, don't try to make more moves
    (-> game
        (game/choose-action move)
        (as-> g
              (if (= :roll-dice (:state g))
                (game/roll g)
                g))
        (assoc :selected-move nil))))

(defn- expand [node]
  (if (= :end-game (:state (:game node)))
    node  ; Don't expand end game states
    (let [moves (game/get-possible-moves (:game node))
          new-children (mapv (fn [move]
                               (->Node (get-next-state (:game node) move)
                                       move
                                       node
                                       []
                                       0
                                       0.0))
                             moves)]
      (assoc node :children new-children))))

(defn- select-best-child [node exploration-param]
  (when (and (seq (:children node)) (not= :end-game (:state (:game node))))
    (let [parent-visits (max 1 (:visits node))]
      (apply max-key
             (fn [child]
               (if (zero? (:visits child))
                 platform/infinity
                 (+ (/ (:value child) (:visits child))
                    (* exploration-param
                       (math/sqrt (/ (math/log parent-visits)
                                     (:visits child)))))))
             (:children node)))))

(defn- backpropagate [node outcome]
  (loop [current node]
    (when current
      (let [updated (-> current
                        (update :visits inc)
                        (update :value + outcome))]
        (if-let [parent (:parent current)]
          (recur (update parent :children
                         (fn [children]
                           (mapv #(if (= (:move %) (:move current)) updated %) children))))
          updated)))))

(defn- simulate [game]
  (loop [state game
         steps 0]
    (if (or (> steps 100) (= :end-game (:state state)))
      (evaluate-state state)
      (case (:state state)
        :roll-dice (recur (game/roll state) (inc steps))
        :choose-action (let [moves (game/get-possible-moves state)]
                         (if (seq moves)
                           (recur (get-next-state state (rand-nth moves)) (inc steps))
                           (recur (assoc state :state :roll-dice :selected-move nil) (inc steps))))
        (recur (assoc state :state :roll-dice :selected-move nil) (inc steps))))))

(defn- tree-policy [node exploration-param]
  (loop [current node]
    (if (= :end-game (:state (:game current)))
      current
      (if (empty? (:children current))
        (let [expanded (expand current)]
          (if (seq (:children expanded))
            (rand-nth (:children expanded))
            expanded))
        (if-let [best-child (select-best-child current exploration-param)]
          (if (zero? (:visits best-child))
            best-child
            (recur best-child))
          current)))))

(defn- mcts-search [root iterations exploration-param]
  (loop [current-root root
         iter iterations]
    (if (zero? iter)
      current-root
      (let [node (tree-policy current-root exploration-param)
            simulation-result (simulate (:game node))
            updated-root (backpropagate node simulation-result)]
        (recur updated-root (dec iter))))))

(defn select-move [game]
  (let [possible-moves (game/get-possible-moves game)]
    (when (seq possible-moves)
      (let [iterations (get-in game [:strategy :params :iterations] 1000)
            exploration-param (get-in game [:strategy :params :exploration] 1.41)
            root (->Node game nil nil [] 0 0.0)
            expanded-root (expand root)
            best-node (mcts-search expanded-root iterations exploration-param)]
        (if-let [best-child (when (seq (:children best-node))
                              (apply max-key :visits (:children best-node)))]
          (:move best-child)
          (rand-nth possible-moves))))))

(defmethod game/select-move :mcts [_ game]
  (select-move game))
