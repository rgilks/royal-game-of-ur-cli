# Monte Carlo Tree Search (MCTS)

## Introduction

Monte Carlo Tree Search (MCTS) is a heuristic search algorithm for some kinds of decision processes, particularly games. MCTS has been particularly successful in games with high branching factors where alpha-beta pruning is less effective. In this implementation of the Royal Game of Ur, MCTS provides a strong and flexible opponent that can adapt to different game situations without requiring extensive domain-specific knowledge.

## How MCTS Works

MCTS works by gradually building up a tree representation of the search space, using random sampling of the search space to guide the search and to provide estimates of the value of moves. The algorithm consists of four main steps:

1. Selection
2. Expansion
3. Simulation
4. Backpropagation

These steps are repeated as many times as computational limits allow, with each iteration improving the accuracy of the tree's value estimates.

## Key Components of MCTS in Our Implementation

### 1. Node Structure

```clojure
(defrecord Node [game move parent children visits value rave-value])
```

This structure represents a node in the MCTS tree, containing:
- The game state
- The move that led to this state
- A reference to the parent node
- A list of child nodes
- Visit count and value statistics
- RAVE (Rapid Action Value Estimation) value for move urgency

### 2. Evaluation Function

```clojure
(defn- score-player [game player]
  (let [off-board (get-in game [:players player :off-board])
        on-board-pieces (game/get-piece-positions (:board game) player)
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
          opponent (game/other-player current-player)
          value (- (score-player game current-player)
                   (score-player game opponent))]
      (cache-evaluation game value)
      value)))
```

This function evaluates the game state, considering:
- The number of pieces off the board (highest priority)
- The number of pieces on the board
- The number of pieces on rosette squares
- The number of pieces ready to exit the board

It calculates this for both players and returns the difference, favoring the current player. The function also uses a transposition table to cache and reuse evaluations of previously seen positions.

### 3. Tree Policy

```clojure
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
```

This function selects the best child node to explore next. It uses a combination of:
- UCT (Upper Confidence Bound for Trees) score
- RAVE score
- A bonus for moves that take pieces off the board

The exploration parameter and RAVE parameter can be tuned to adjust the balance between exploration and exploitation.

### 4. Simulation

```clojure
(defn- simulate [game]
  (loop [state game
         steps 0
         moves []]
    (if (or (> steps 100) (= :end-game (:state state)))
      [(evaluate-state state) moves]
      (case (:state state)
        :roll-dice (recur (game/roll state) (inc steps) moves)
        :choose-action (let [possible-moves (game/get-possible-moves state)]
                         (if (seq possible-moves)
                           (let [move (rand-nth possible-moves)]
                             (recur (get-next-state state move) (inc steps) (conj moves move)))
                           (recur (assoc state :state :roll-dice :selected-move nil) (inc steps) moves)))
        (recur (assoc state :state :roll-dice :selected-move nil) (inc steps) moves)))))
```

This function performs a random playout from a given state to the end of the game or until a maximum number of steps is reached. It returns the final state evaluation and the sequence of moves made during the simulation.

### 5. Backpropagation

```clojure
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
```

This function updates the statistics of all nodes along the path from the simulated leaf to the root. It also updates RAVE values for siblings of nodes in the selected path.

### 6. Parallel MCTS

```clojure
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
```

This function implements a parallel version of MCTS, utilizing multiple threads to perform searches concurrently. This significantly improves the performance of the algorithm, especially on multi-core systems.

## MCTS in Action

The main MCTS algorithm is implemented in the `select-move` function:

```clojure
(defn select-move [game]
  (let [possible-moves (game/get-possible-moves game)]
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
```

This function initiates the MCTS process, performs the search, and selects the best move based on the most visited child of the root node.

## Strengths and Weaknesses of MCTS

Strengths:
- Can handle games with high branching factors
- Does not require a sophisticated evaluation function
- Can be stopped anytime and will have a best move available
- Naturally balances exploration and exploitation
- Parallelizes well

Weaknesses:
- May perform poorly with a limited number of simulations
- Can be computationally expensive, especially for complex games
- May struggle in games with very uneven distributions of rewards

## Tuning MCTS

The performance of MCTS can be tuned by adjusting several parameters:

1. Number of iterations: More iterations generally lead to better performance but require more computation time.
2. Exploration parameter: Higher values encourage more exploration, while lower values favor exploitation.
3. RAVE parameter: Affects how quickly the algorithm shifts from using RAVE values to using UCT values.

## References

1. Browne, C. B., et al. (2012). A survey of monte carlo tree search methods. IEEE Transactions on Computational Intelligence and AI in games, 4(1), 1-43.
2. Gelly, S., & Silver, D. (2011). Monte-Carlo tree search and rapid action value estimation in computer Go. Artificial Intelligence, 175(11), 1856-1875.
3. Chaslot, G. M. J. B., Winands, M. H., & van Den Herik, H. J. (2008). Parallel monte-carlo tree search. In Computers and Games (pp. 60-71). Springer, Berlin, Heidelberg.
