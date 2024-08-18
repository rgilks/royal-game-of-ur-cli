# The Minimax Algorithm

The minimax algorithm is a decision-making technique used in artificial intelligence for two-player turn-based games. It's particularly effective in games with perfect information, where both players have complete knowledge of the game state. Here we'll look at how minimax works and how it's implemented.

## What is Minimax?

Minimax is a recursive algorithm that simulates all possible moves in a game, assuming that both players play optimally. The algorithm alternates between two players:

1. The maximizing player (usually the AI) tries to maximize their score.
2. The minimizing player (usually the opponent) tries to minimize the AI's score.

The algorithm explores the game tree to a certain depth, evaluates the resulting positions, and chooses the move that leads to the best outcome assuming the opponent also plays optimally.

```mermaid
graph TD
    A[Max] --> B[Min]
    A --> C[Min]
    B --> D[Max]
    B --> E[Max]
    C --> F[Max]
    C --> G[Max]
    D --> H(3)
    D --> I(5)
    E --> J(6)
    E --> K(9)
    F --> L(1)
    F --> M(2)
    G --> N(0)
    G --> O(7)
    
    style A fill:#f9f,stroke:#333,stroke-width:4px,color:#000
    style B fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style C fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style D fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style E fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style F fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style G fill:#f9f,stroke:#333,stroke-width:2px,color:#000
```

## Key Components of Minimax

1. **Evaluation Function**: This function assigns a score to a game state. In our implementation, it's the `evaluate-state` function.

2. **Depth Limit**: To prevent the algorithm from exploring infinitely, we set a maximum depth. In our code, it's defined by `max-depth`.

3. **Recursion**: The algorithm recursively explores possible moves, alternating between maximizing and minimizing at each level.

4. **Alpha-Beta Pruning**: An optimization technique that reduces the number of nodes evaluated in the search tree.

## Minimax in the Royal Game of Ur

Let's break down the implementation:

### Evaluation Function

```clojure
(defn- score-player [game-state player]
  (+ (* 10 (get-in game-state [:players player :off-board]))
     (count (state/get-piece-positions (:board game-state) player))))

(defn- evaluate-state [game-state]
  (let [current-player (:current-player game-state)
        opponent (state/other-player current-player)]
    (- (score-player game-state current-player)
       (score-player game-state opponent))))
```

This function evaluates the game state by considering:
- The number of pieces that have completed the board (multiplied by 10 for higher importance)
- The number of pieces on the board

It calculates this for both players and returns the difference, favoring the current player.

```mermaid
graph LR
    subgraph Player A Path
    A1[1] --> A2[2] --> A3[3] --> A4((4))
    A4 --> B1[5] --> B2[6] --> B3[7] --> B4((8))
    B4 --> C1[9] --> C2[10] --> C3[11] --> C4[12] --> C5[13] --> C6[14] --> C7((15))
    end
    
    subgraph Player B Path
    D1[1] --> D2[2] --> D3[3] --> D4((4))
    D4 --> B1
    C7 --> E1[16] --> E2((17))
    end
    
    style A4 fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style B4 fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style C7 fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style D4 fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style E2 fill:#f9f,stroke:#333,stroke-width:2px,color:#000
```

### The Minimax Function

```clojure
(defn- minimax [game-state depth maximizing? alpha beta]
  (if (or (zero? depth) (= :end-game (:state game-state)))
    [(evaluate-state game-state) nil]
    (let [moves (safe-get-moves game-state)
          init-score (if maximizing? (- platform/infinity) platform/infinity)
          comparator (if maximizing? > <)]
      ; ... (rest of the function)
```

This function is the core of the algorithm:

1. It first checks if we've reached the maximum depth or the end of the game. If so, it evaluates the current state.
2. If not, it gets all possible moves and initializes the best score based on whether we're maximizing or minimizing.
3. It then loops through all possible moves, recursively calling itself for each move.
4. For each move, it updates the best score and move if a better option is found.
5. It uses alpha-beta pruning to optimize the search.

```mermaid
graph TD
    A[Max: Initial State] --> B[Min: Move 1]
    A --> C[Min: Move 2]
    B --> D[Max: Move 1.1]
    B --> E[Max: Move 1.2]
    C --> F[Max: Move 2.1]
    C --> G[Max: Move 2.2]
    D --> H[Min: Move 1.1.1]
    D --> I[Min: Move 1.1.2]
    E --> J[Min: Move 1.2.1]
    E --> K[Min: Move 1.2.2]
    H --> L((Eval))
    I --> M((Eval))
    J --> N((Eval))
    K --> O((Eval))
    
    style A fill:#f9f,stroke:#333,stroke-width:4px,color:#000
    style B fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style C fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style D fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style E fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style F fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style G fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style H fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style I fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style J fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style K fill:#bbf,stroke:#333,stroke-width:2px,color:#000
```

### Alpha-Beta Pruning

Alpha-beta pruning is an optimization technique that significantly reduces the number of nodes evaluated in the search tree. It works by maintaining two values, alpha and beta:

- Alpha is the best value that the maximizer currently can guarantee at that level or above.
- Beta is the best value that the minimizer currently can guarantee at that level or above.

If at any point beta becomes less than or equal to alpha, the rest of that branch can be pruned because it won't influence the final decision.

```mermaid
graph TD
    A[Max: α=-∞, β=+∞] --> B[Min: α=-∞, β=+∞]
    A --> C[Min: α=5, β=+∞]
    B --> D[Max: α=-∞, β=+∞]
    B --> E[Max: α=-∞, β=3]
    C --> F[Max: α=5, β=+∞]
    C --> G[Pruned]
    D --> H((3))
    D --> I((5))
    E --> J((2))
    E --> K[Pruned]
    F --> L((7))
    F --> M[Pruned]
    
    style A fill:#f9f,stroke:#333,stroke-width:4px,color:#000
    style B fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style C fill:#bbf,stroke:#333,stroke-width:2px,color:#000
    style D fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style E fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style F fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style G fill:#ddd,stroke:#333,stroke-width:2px,color:#000
    style K fill:#ddd,stroke:#333,stroke-width:2px,color:#000
    style M fill:#ddd,stroke:#333,stroke-width:2px,color:#000
```

### Move Selection

```clojure
(defn select-move [possible-moves game-state]
  (when (seq possible-moves)
    (second (minimax game-state max-depth true (- platform/infinity) platform/infinity))))
```

This function initiates the minimax algorithm and returns the best move found.

## Performance Considerations

While minimax with alpha-beta pruning is powerful, its performance can be further improved:

1. **Move Ordering**: By considering promising moves first, we can improve the efficiency of alpha-beta pruning[^1].
2. **Transposition Tables**: Storing and reusing evaluations of previously seen positions can prevent redundant calculations[^2].
3. **Iterative Deepening**: This technique allows the algorithm to make the best use of available time by progressively increasing the search depth[^3].

## Conclusion

The minimax algorithm with alpha-beta pruning provides an effectice way for an AI to make decisions in turn-based games. It allows the AI to look several moves ahead and choose the best possible move, considering both its own opportunities and the opponent's potential responses.

While this implementation provides a strong AI player, it's worth noting that the effectiveness of the minimax algorithm heavily depends on the accuracy of the evaluation function and the depth of the search. Deeper searches generally lead to stronger play but require more computational resources.

## Further Reading

- For a deeper dive into game theory and minimax, see "Artificial Intelligence: A Modern Approach" by Stuart Russell and Peter Norvig[^4].
- To explore more about the Royal Game of Ur and its historical context, check out Irving Finkel's work on the game[^5].

## References

[^1]: Millington, I., & Funge, J. (2009). Artificial Intelligence for Games (2nd ed.). Morgan Kaufmann Publishers.

[^2]: Schaeffer, J. (1989). The history heuristic and alpha-beta search enhancements in practice. IEEE Transactions on Pattern Analysis and Machine Intelligence, 11(11), 1203-1212.

[^3]: Korf, R. E. (1985). Depth-first iterative-deepening: An optimal admissible tree search. Artificial Intelligence, 27(1), 97-109.

[^4]: Russell, S., & Norvig, P. (2020). Artificial Intelligence: A Modern Approach (4th ed.). Pearson.

[^5]: Finkel, I. (2007). On the rules for the Royal Game of Ur. In Ancient Board Games in Perspective. British Museum Press.