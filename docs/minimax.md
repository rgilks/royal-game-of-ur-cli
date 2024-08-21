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
  (+ (* 10 (get-in game [:players player :off-board]))
     (count (state/get-piece-positions (:board game) player))))

(defn- evaluate-state [game]
  (let [current-player (:current-player game)
        opponent (state/other-player current-player)]
    (- (score-player game current-player)
       (score-player game opponent))))
```

This function evaluates the game state by considering:
- The number of pieces that have completed the board (multiplied by 10 for higher importance)
- The number of pieces on the board

It calculates this for both players and returns the difference, favoring the current player.

### 2. Move Generation

```clojure
(defn- get-next-state [game move]
  (-> game
      (state/choose-action move)
      (assoc :roll (reduce + (repeatedly 4 #(rand-int 2))))))

(defn- safe-get-moves [game]
  (if (= (:state game) :choose-action)
    (state/get-moves game)
    []))
```

These functions generate possible moves and apply them to create new game states.

### 3. The Minimax Function

```clojure
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
                (recur rest-moves new-best-score new-best-move new-alpha new-beta))))))))
```

This is the core of the algorithm:

1. It first checks if we've reached the maximum depth or the end of the game. If so, it evaluates the current state.
2. If not, it gets all possible moves and initializes the best score based on whether we're maximizing or minimizing.
3. It then loops through all possible moves, recursively calling itself for each move.
4. For each move, it updates the best score and move if a better option is found.
5. It uses alpha-beta pruning to optimize the search.

### 4. Alpha-Beta Pruning

Alpha-beta pruning is an optimization technique that significantly reduces the number of nodes evaluated in the search tree. It works by maintaining two values, alpha and beta:

- Alpha is the best value that the maximizer currently can guarantee at that level or above.
- Beta is the best value that the minimizer currently can guarantee at that level or above.

If at any point beta becomes less than or equal to alpha, the rest of that branch can be pruned because it won't influence the final decision.

### 5. Move Selection

```clojure
(defn select-move [game]
  (when (seq (state/get-possible-moves game))
    (let [depth (get-in game [:strategy :params :depth] 3)]
      (second (minimax game depth true (- platform/infinity) platform/infinity)))))
```

This function initiates the minimax algorithm and returns the best move found. The search depth is configurable through the game's strategy parameters.

## Performance Considerations

While minimax with alpha-beta pruning is powerful, its performance can be further improved:

1. **Move Ordering**: By considering promising moves first, we can improve the efficiency of alpha-beta pruning.
2. **Transposition Tables**: Storing and reusing evaluations of previously seen positions can prevent redundant calculations.
3. **Iterative Deepening**: This technique allows the algorithm to make the best use of available time by progressively increasing the search depth.

## References

1. Russell, S., & Norvig, P. (2020). Artificial Intelligence: A Modern Approach (4th ed.). Pearson.
2. Millington, I., & Funge, J. (2009). Artificial Intelligence for Games (2nd ed.). Morgan Kaufmann Publishers.
3. Knuth, D. E., & Moore, R. W. (1975). An analysis of alpha-beta pruning. Artificial Intelligence, 6(4), 293-326.
   