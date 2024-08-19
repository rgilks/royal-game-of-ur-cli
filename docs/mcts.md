# Monte Carlo Tree Search (MCTS)

## Introduction

Monte Carlo Tree Search (MCTS) is a heuristic search algorithm for some kinds of decision processes, particularly games. MCTS has been particularly successful in games with high branching factors where alpha-beta pruning is less effective, such as Go. In our implementation of the Royal Game of Ur, MCTS provides a strong and flexible opponent.

## How MCTS Works

MCTS works by gradually building up a tree representation of the search space, using random sampling of the search space to guide the search and to provide estimates of the value of moves. The algorithm consists of four main steps:

1. Selection
2. Expansion
3. Simulation
4. Backpropagation

These steps are repeated as many times as computational limits allow, with each iteration improving the accuracy of the tree's value estimates.

### 1. Selection

Starting at the root node, the algorithm recursively selects child nodes until a leaf node is reached. The selection of which child node to choose is based on the Upper Confidence Bound for Trees (UCT) formula:

```
UCT = Vi + C * sqrt(ln(N) / ni)
```

Where:
- Vi is the value estimate of the child node
- N is the number of times the parent node has been visited
- ni is the number of times the child node has been visited
- C is an exploration parameter (typically sqrt(2))

This formula balances exploitation (choosing nodes with high value estimates) and exploration (choosing nodes that have been visited less frequently).

### 2. Expansion

If the selected leaf node is not a terminal game state and has been visited before, one or more child nodes are added to expand the tree.

### 3. Simulation

From the new node, a simulation (also called a rollout) is played out to the end of the game. In our implementation, this is done by choosing random moves for both players until the game ends.

### 4. Backpropagation

The result of the simulation is then backpropagated up the tree, updating the statistics (visit counts and value estimates) of all nodes along the path from the expanded node to the root.

## Implementation in the Royal Game of Ur

In our implementation, we use the following key components:

1. `Node` record: Represents a node in the MCTS tree, containing:
   - The game state
   - The move that led to this state
   - A reference to the parent node
   - A list of child nodes
   - Visit count and value statistics

2. `expand` function: Creates child nodes for all possible moves from a given game state.

3. `select-best-child` function: Uses the UCT formula to select the best child node to explore.

4. `simulate` function: Plays out a random game from a given state to completion.

5. `backpropagate` function: Updates the statistics of all nodes along the path from a leaf to the root.

6. `mcts-search` function: The main MCTS algorithm, which repeatedly performs selection, expansion, simulation, and backpropagation for a specified number of iterations.

## Strengths and Weaknesses

Strengths:
- Can handle games with high branching factors
- Does not require a sophisticated evaluation function
- Can be stopped anytime and will have a best move available
- Naturally balances exploration and exploitation

Weaknesses:
- May perform poorly with a limited number of simulations
- Can be computationally expensive, especially for complex games
- May struggle in games with very uneven distributions of rewards

## Tuning MCTS

The performance of MCTS can be tuned by adjusting several parameters:

1. Number of iterations: More iterations generally lead to better performance but require more computation time.
2. Exploration parameter (C in the UCT formula): Higher values encourage more exploration, while lower values favor exploitation.
3. Simulation strategy: While our implementation uses random playouts, more sophisticated simulation strategies can sometimes improve performance.

## Conclusion

MCTS provides a powerful and flexible approach to game AI. In the context of the Royal Game of Ur, it offers a strong opponent that can adapt to different game situations without requiring extensive domain-specific knowledge. As you experiment with the AI, try adjusting the MCTS parameters to see how they affect its play style and strength.

## References

1. Browne, C. B., et al. (2012). A survey of monte carlo tree search methods. IEEE Transactions on Computational Intelligence and AI in games, 4(1), 1-43.
2. Coulom, R. (2006). Efficient selectivity and backup operators in Monte-Carlo tree search. In International conference on computers and games (pp. 72-83). Springer, Berlin, Heidelberg.
3. Kocsis, L., & Szepesvári, C. (2006). Bandit based monte-carlo planning. In European conference on machine learning (pp. 282-293). Springer, Berlin, Heidelberg.
