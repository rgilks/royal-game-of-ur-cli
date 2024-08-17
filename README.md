# Royal Game of Ur

![Royal Game of Ur Console](./docs/screen.png)

## Introduction

The Royal Game of Ur, also known as the Game of Twenty Squares, is one of the oldest known board games, dating back to around 2600 BCE. This project implements a digital version of this ancient game, complete with a command-line interface and a robust state machine design.

## Features

- Faithful implementation of the Royal Game of Ur rules
- Command-line interface for easy gameplay
- Robust state machine design for game logic
- AI opponent with configurable strategies
- Comprehensive test suite
- Colorized output for enhanced user experience
- Simulation mode for running multiple games with different strategies

## Getting Started

### Prerequisites

This project uses version management to ensure consistent development environments. We recommend using `asdf` as the version manager. The required tool versions are specified in the `.tool-versions` file.

Ensure you have the following tools installed on your system:

- asdf
- Clojure
- Node.js
- Yarn
- Java (GraalVM)
- Just (command runner)
- Graphviz (for generating state diagrams)

Note that this project was developed on a Mac, so some commands may differ on other operating systems.

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/royal-game-of-ur.git
   cd royal-game-of-ur
   ```

2. Initialize the project:
   ```
   just init
   ```

   This command will:
   - Set up asdf and install required plugins and versions
   - Install necessary tools (including Graphviz, nbb, and GraalVM)
   - Set up Git hooks
   - Install project dependencies
   - Perform any other necessary initialization steps

## Development

This project uses `just` as a command runner. To see all available commands, run:

```
just
```

Some useful commands include:

- `just cli`: Run the application
- `just watch`: Run unit tests and watch for changes
- `just test`: Run unit tests
- `just fmt`: Format Clojure files
- `just concat`: Concatenate all project files
- `just state-diagram`: Generate the state diagram
- `just sim`: Run a simulation with custom parameters
- `just build`: Build the project (creates uberjar and native image)

To update all tools to their latest versions:

```
just update-tools
```

## Usage

To start a new game:

```
just cli
```

This will launch the game in your terminal. Follow the on-screen instructions to play.

To run a simulation of multiple games with different strategies:

```
just sim [num_games] [strategy_a] [strategy_b] [debug] [show] [delay]
```

For example:
```
just sim 1000 strategic random true false 0
```

## Game Rules

1. The game is played on a board with 20 squares arranged in three rows.
2. Each player has 7 pieces that they need to move from their starting position, across the board, and off the other side.
3. Players roll four tetrahedral dice to determine their move (simulated in this digital version).
4. Players can choose to either bring a new piece onto the board or move an existing piece.
5. If a player lands on an opponent's piece, that piece is captured and must start over.
6. Landing on a rosette square grants an extra turn.
7. Pieces must leave the board on an exact roll.
8. The first player to move all seven pieces off the board wins.

![Royal Game of Ur Board](./docs/board.png)
![Royal Game of Ur State Machine](./docs/rgou-fsm.png)

## Project Structure

- `src/`: Source code files
- `test/`: Test files
- `docs/`: Documentation files, including game board and state machine diagrams
- `scripts/`: Utility scripts for development
- `justfile`: Command runner file with various development tasks
- `.tool-versions`: Specifies the versions of tools used in the project

## Testing

To run the test suite:

```
just test
```

For continuous testing during development:

```
just watch
```

## Building

To build the project (create uberjar and native image):

```
just build
```

After building, you can run the game with:

```
./royal-game-of-ur
```

## License

This project is open source and available under the [MIT License](LICENSE).
