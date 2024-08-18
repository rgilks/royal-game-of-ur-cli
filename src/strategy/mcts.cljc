(ns strategy.mcts
  (:require [config]
            [state :as state]))

(defn debug [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn- score-player [game-state player]
  (+ (* 10 (get-in game-state [:players player :off-board]))
     (count (state/get-piece-positions (:board game-state) player))))

(defn- get-next-state [game-state move]
  (debug "Getting next state for move:" move)
  (-> game-state
      (state/choose-action move)
      (assoc :state :roll-dice)
      (assoc :roll nil)
      (assoc :selected-move nil)))

(defn- get-legal-moves [game-state]
  (let [moves (if (= (:state game-state) :choose-action)
                (state/get-moves game-state)
                [])]
    (debug "Legal moves:" moves)
    moves))

(defrecord Node [game-state move parent children visits value])

(defn- expand [node]
  (debug "Expanding node")
  (let [moves (get-legal-moves (:game-state node))
        new-children (mapv (fn [move]
                             (->Node (get-next-state (:game-state node) move)
                                     move
                                     node
                                     []
                                     0
                                     0.0))
                           moves)]
    (debug "Created" (count new-children) "child nodes")
    (assoc node :children new-children)))

(defn- select-best-child [node exploration-param]
  (debug "Selecting best child with exploration param:" exploration-param)
  (let [parent-visits (max 1 (:visits node))
        best-child (apply max-key
                          (fn [child]
                            (+ (/ (:value child) (max 1 (:visits child)))
                               (* exploration-param
                                  (Math/sqrt (/ (Math/log parent-visits)
                                                (max 1 (:visits child)))))))
                          (:children node))]
    (debug "Selected child with move:" (:move best-child))
    best-child))

(defn- backpropagate [node outcome]
  (debug "Backpropagating outcome:" outcome)
  (loop [current node]
    (when current
      (let [updated (-> current
                        (update :visits inc)
                        (update :value + outcome))]
        (if-let [parent (:parent current)]
          (recur (assoc parent
                        :children
                        (mapv #(if (= (:move %) (:move current)) updated %)
                              (:children parent))))
          updated)))))

(defn- simulate [game-state]
  (debug "Starting simulation")
  (loop [state game-state
         steps 0]
    (if (> steps 1000)  ; Prevent infinite loops
      (do (debug "Simulation exceeded 1000 steps, terminating")
          0)
      (case (:state state)
        :end-game (do (debug "Reached end game state")
                      (if (= :A (:current-player state)) 1 0))
        :roll-dice (do (debug "Rolling dice")
                       (recur (state/dice-roll state) (inc steps)))
        :choose-action (let [moves (get-legal-moves state)]
                         (if (seq moves)
                           (let [chosen-move (rand-nth moves)]
                             (debug "Choosing random move:" chosen-move)
                             (recur (get-next-state state chosen-move) (inc steps)))
                           (do (debug "No moves available, switching to roll dice")
                               (recur (assoc state :state :roll-dice :selected-move nil) (inc steps)))))
        (do (debug "Unknown state, switching to roll dice")
            (recur (assoc state :state :roll-dice :selected-move nil) (inc steps)))))))

(defn- mcts-search [root iterations exploration-param]
  (debug "Starting MCTS search with" iterations "iterations")
  (loop [current-root root
         iter iterations]
    (if (zero? iter)
      (do (debug "MCTS search completed")
          current-root)
      (let [node (loop [node current-root]
                   (if (seq (:children node))
                     (if (every? #(pos? (:visits %)) (:children node))
                       (recur (select-best-child node exploration-param))
                       node)
                     node))
            expanded-node (if (zero? (:visits node)) node (expand node))
            simulation-result (simulate (:game-state expanded-node))
            updated-root (backpropagate expanded-node simulation-result)]
        (when (zero? (mod iter 100))
          (debug iter "iterations remaining"))
        (recur updated-root (dec iter))))))

(defn select-move [possible-moves game-state]
  (debug "Selecting move from" (count possible-moves) "possible moves")
  (when (seq possible-moves)
    (let [iterations (get-in game-state [:strategy :params :iterations] 1000)
          exploration-param (get-in game-state [:strategy :params :exploration] 1.41)
          root (->Node (assoc game-state :selected-move nil) nil nil [] 0 0.0)
          expanded-root (expand root)
          best-node (mcts-search expanded-root iterations exploration-param)
          best-child (apply max-key :visits (:children best-node))]
      (debug "Selected move:" (:move best-child))
      (:move best-child))))

(defmethod state/select-move :mcts [_ possible-moves game-state]
  (select-move possible-moves game-state))
