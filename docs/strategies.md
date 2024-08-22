# AI Strategies 

## Introduction

The Royal Game of Ur implementation features several AI strategies, each with its own strengths and characteristics. This document provides an overview of these strategies, their basic principles, and when they might be most effective.

## Available Strategies

### 1. Minimax

**Description**: Minimax is a decision-making algorithm used in two-player turn-based games. It works by simulating all possible moves to a certain depth, alternating between maximizing the AI's score and minimizing the opponent's score.

**Key Features**:
- Looks ahead several moves
- Considers both its own opportunities and the opponent's potential responses
- Uses alpha-beta pruning for improved efficiency

**Best Used When**: The game state is not too complex, and you want an AI that can plan ahead and play defensively.

**Customizable Parameters**:
- `depth`: The number of moves to look ahead. Higher values result in stronger play but require more computation time.

For more details, see the [Minimax Algorithm Documentation](./minimax.md).

### 2. Monte Carlo Tree Search (MCTS)

**Description**: MCTS is a probabilistic search algorithm that balances exploration and exploitation to find optimal moves. It builds a search tree gradually by simulating random games from the current position.

**Key Features**:
- Adapts well to different game situations
- Does not require an extensive evaluation function
- Can handle games with high branching factors
- Parallelizes well for improved performance

**Best Used When**: The game has a large number of possible moves, or when you want an AI that can adapt to unusual game states.

**Customizable Parameters**:
- `iterations`: The number of simulations to run. More iterations generally lead to better performance but require more computation time.
- `exploration`: Controls the balance between exploration and exploitation in the search.

For more details, see the [Monte Carlo Tree Search (MCTS) Documentation](./mcts.md).

### 3. Random

**Description**: The Random strategy simply chooses a move at random from all available legal moves.

**Key Features**:
- Very fast
- Unpredictable
- Serves as a baseline for comparing other strategies.

### 4. First-in-list

**Description**: This strategy always chooses the first available move from the list of legal moves. With the pieces ordered with those at the start of the path first.

**Key Features**:
- Deterministic (always makes the same choice in the same situation)
- Very fast
- Serves as a simple baseline strategy
- Very effective against other simple strategies 

**Best Used When**: You want a consistent, predictable opponent for testing or learning the game basics.

### 5. Strategic

**Description**: The Strategic strategy uses a simple heuristic to prioritize moves based on basic game principles.

**Key Features**:
- Faster than Minimax or MCTS
- Makes "sensible" moves without deep calculation
- Prioritizes key strategic elements of the game

**Priorities (in order)**:
1. Move a piece off the board if possible
2. Move to the last square on the path
3. Capture an opponent's piece
4. Move to a rosette
5. Move the piece furthest back

**Best Used When**: You want an AI that plays a decent game without the computational overhead of more complex strategies. First in list is better however.

## Comparing Strategies

The effectiveness of each strategy can vary depending on the specific game situation and the parameters used. In general:

- Minimax and MCTS tend to be the strongest strategies, especially with higher depth/iteration settings.
- The Strategic strategy provides a good balance of reasonable play and quick decision-making.
- Random and First-in-list strategies are primarily useful for testing and providing a baseline for comparison.

You can use the simulation mode to compare the performance of different strategies. For example:

```
just sim num-games=100 strategy-A=minimax strategy-A-depth=3 strategy-B=first-in-list debug=false show=false parallel=6 validate=false
```

This will run 1000 games pitting the Minimax strategy (with depth 3) against the 'first in list' strategy.

## Running Simulations

To run a simulation, use the following command format:

```
just sim [parameters]
```

Available parameters:
- `num-games`: Number of games to simulate
- `strategy-A`: Strategy for Player A
- `strategy-B`: Strategy for Player B 
- `debug`: Enable debug mode
- `show`: Show game state
- `parallel`: Number of parallel threads to use
- `validate`: Enable validation

Strategy-specific parameters can be set using the format `strategy-X-param=value`, where X is A or B, and param is the parameter name. For example:

- `strategy-A-depth=3` sets the depth parameter for Player A's minimax strategy
- `strategy-B-iterations=1000` sets the iterations parameter for Player B's MCTS strategy

## Implementing New Strategies

If you're interested in implementing a new strategy:

1. Create a new file in the `src/strategy` directory (e.g., `my_strategy.cljc`).
2. Implement the `select-move` multimethod for your strategy.
3. Add your strategy to the options in the simulation configuration.
