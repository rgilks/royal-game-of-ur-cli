# AI Strategies in the Royal Game of Ur

This document provides an overview of the AI strategies implemented in our Royal Game of Ur project.

## 1. Minimax Algorithm

The Minimax algorithm is a decision-making algorithm used in two-player turn-based games. It works by simulating all possible moves and their outcomes, assuming that both players play optimally.

### How it works:

1. The algorithm explores the game tree to a certain depth.
2. It alternates between maximizing the AI's score and minimizing the opponent's score.
3. At the maximum depth, it evaluates the game state using a heuristic function.
4. The best move is chosen based on these evaluations.

Our implementation includes alpha-beta pruning for improved efficiency.

For more details, see our [Minimax Algorithm Documentation](./minimax.md).

## 2. Monte Carlo Tree Search (MCTS)

Monte Carlo Tree Search is a probabilistic search algorithm that balances exploration and exploitation to find optimal moves.

### How it works:

1. Selection: Starting from the root, select successive child nodes down to a leaf node.
2. Expansion: If the leaf node is not a terminal state, create one or more child nodes.
3. Simulation: Perform a random playout from the new node(s).
4. Backpropagation: Use the result of the playout to update information in the nodes on the path from the new node(s) to the root.

Our implementation uses the UCT (Upper Confidence Bound for Trees) formula for node selection.

For more details, see our [MCTS Documentation](./mcts.md).

## 3. Random Strategy

The Random strategy simply chooses a move at random from all available legal moves. While not a strong player, it serves as a baseline for comparing other strategies and can sometimes produce unexpected results.

## 4. First-in-list Strategy

This strategy always chooses the first available move from the list of legal moves. It's a deterministic strategy that doesn't consider the game state beyond the immediate legal moves.

## 5. Strategic Strategy

The Strategic strategy uses a simple heuristic to prioritize moves. The priorities are:

1. Move a piece off the board if possible
2. Move to the last square on the path
3. Capture an opponent's piece
4. Move to a rosette
5. Move the piece furthest back

This strategy provides a balance between simplicity and some basic game understanding.

## Comparing Strategies

We use our simulation mode to compare the performance of different strategies. You can run your own comparisons using the `just sim` command. For example:

```
just sim 1000 minimax mcts true false 0
```

This will run 1000 games pitting the Minimax strategy against the MCTS strategy, with debug output enabled.

## Implementing New Strategies

To implement a new strategy:

1. Create a new file in the `src/strategy` directory (e.g., `my_strategy.cljc`).
2. Implement the `select-move` multimethod for your strategy.
3. Add your strategy to the options in the simulation configuration.

We welcome contributions of new strategies!