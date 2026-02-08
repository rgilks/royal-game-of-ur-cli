# The Minimax Algorithm

## Introduction

The Minimax algorithm is a decision-making technique used in artificial intelligence for two-player turn-based games. It's particularly effective in games with perfect information, where both players have complete knowledge of the game state.

## How Minimax Works

Minimax is a recursive algorithm that simulates all possible moves in a game, assuming that both players play optimally. The algorithm alternates between two players:

1. The maximizing player (usually the AI) tries to maximize their score.
2. The minimizing player (usually the opponent) tries to minimize the AI's score.

The algorithm explores the game tree to a certain depth, evaluates the resulting positions, and chooses the move that leads to the best outcome assuming the opponent also plays optimally.

## Key Components of Minimax in Our Implementation

### 1. Evaluation Function

```clojure
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
```

This function evaluates the game state by considering:
- The number of pieces off the board (highest priority, weighted 100)
- The number of pieces on the board (weighted 10)
- The number of pieces on rosette squares (weighted 5)
- The number of pieces ready to exit (weighted 50)

It calculates this for both players and returns the difference, favoring the current player.

### 2. Move Generation

```clojure
(defn- simulate-roll [game roll]
  (-> game
      (assoc :roll roll)
      (assoc :state :choose-action)))

(defn- safe-get-moves [game]
  (if (= (:state game) :choose-action)
    (engine/get-moves game)
    []))
```

These functions generate possible moves and simulate dice rolls to create new game states. The minimax implementation considers all possible dice rolls (0-4) weighted by their dampened probabilities.

### 3. The Minimax Function

The core minimax function uses alpha-beta pruning and considers all possible dice outcomes at each step, weighted by dampened probabilities:

1. It first checks if we've reached the maximum depth or the end of the game. If so, it evaluates the current state.
2. If no moves are available, it computes the expected value across all dice rolls for the opponent's turn.
3. If moves are available, it loops through all possible moves, computing each move's expected value across all dice outcomes.
4. For each move, it updates the best score and move if a better option is found.
5. It uses alpha-beta pruning to cut off branches that can't improve the result.
6. Results are cached in a transposition table to avoid redundant calculations.

### 4. Alpha-Beta Pruning

Alpha-beta pruning is an optimization technique that significantly reduces the number of nodes evaluated in the search tree. It works by maintaining two values, alpha and beta:

- Alpha is the best value that the maximizer currently can guarantee at that level or above.
- Beta is the best value that the minimizer currently can guarantee at that level or above.

If at any point beta becomes less than or equal to alpha, the rest of that branch can be pruned because it won't influence the final decision.

### 5. Move Selection

```clojure
(defn select-move [game]
  (when (seq (engine/get-possible-moves game))
    (let [base-depth (get-in game [:strategy :params :depth] 3)
          depth (adaptive-depth game base-depth)
          damp (get-in game [:strategy :params :damp] 0.5)
          [score best-move] (minimax game depth true (- platform/infinity) platform/infinity damp)]
      best-move)))
```

This function initiates the minimax algorithm and returns the best move found. The search depth is adaptive based on the number of pieces remaining in the game, and dice probability dampening is configurable through the `:damp` strategy parameter.

## Performance Optimizations

This implementation includes several performance optimizations:

1. **Move Ordering**: Moves are sorted by the resulting score for the current player, ensuring promising moves are evaluated first for better alpha-beta pruning.
2. **Transposition Table**: A cache stores evaluations of previously seen board positions to prevent redundant calculations.
3. **Adaptive Depth**: The search depth dynamically increases as pieces leave the hand, deepening the search in the endgame when the branching factor is lower.
4. **Dice Probability Dampening**: The `:damp` parameter allows flattening the dice probability distribution, reducing the impact of unlikely rolls on the evaluation.

## References

1. Russell, S., & Norvig, P. (2020). Artificial Intelligence: A Modern Approach (4th ed.). Pearson.
2. Millington, I., & Funge, J. (2009). Artificial Intelligence for Games (2nd ed.). Morgan Kaufmann Publishers.
3. Knuth, D. E., & Moore, R. W. (1975). An analysis of alpha-beta pruning. Artificial Intelligence, 6(4), 293-326.
   