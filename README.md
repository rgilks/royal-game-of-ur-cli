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

## Technologies Used

This project utilizes a variety of technologies and tools:

1. [Clojure](https://clojure.org/): A dynamic, functional programming language for the JVM, used as the primary language for the project.
2. [ClojureScript](https://clojurescript.org/): A compiler for Clojure that targets JavaScript, allowing the game to run in browser environments.
3. [nbb](https://github.com/babashka/nbb): A scripting environment for ClojureScript, used for running ClojureScript without compilation.
4. [Malli](https://github.com/metosin/malli): A data-driven schema library for Clojure(Script), used for data validation and schema definitions.
5. [core.async](https://github.com/clojure/core.async): A Clojure(Script) library for asynchronous programming, used for running simulations concurrently.
6. [JLine](https://github.com/jline/jline3): A Java library for handling console input, used for improved terminal interactions in the Clojure version.
7. [readline-sync](https://github.com/anseki/readline-sync): A npm package for synchronous readline in Node.js, used for console input in the ClojureScript version.
8. [just](https://github.com/casey/just): A command runner used for various development tasks and running the game.
9. [asdf](https://asdf-vm.com/): A version manager for multiple runtime versions, used to manage Clojure, Node.js, and other tool versions.
10. [yarn](https://yarnpkg.com/): A package manager for JavaScript, used to manage Node.js dependencies.
11. [GraalVM](https://www.graalvm.org/): A high-performance JDK distribution, used for creating native executables of the game.
12. [Graphviz](https://graphviz.org/): An open-source graph visualization software, used for generating state diagrams.

## Getting Started

### Prerequisites

This project was developed on a Mac (specifically an M1 Mac). Users on different operating systems may need to adapt these instructions to their environment.

Before you begin, make sure you have the following installed:

1. **Homebrew**: If you don't have Homebrew installed, run the following command in your terminal:

   ```
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

   After installation, make sure to follow the instructions in the terminal to add Homebrew to your PATH.

2. **Git**: Install Git using Homebrew:
   
   ```
   brew install git
   ```

3. **asdf**: Install asdf using Homebrew:

   ```
   brew install asdf
   ```

   After installation, add the following to your `~/.zshrc` or `~/.bash_profile`:

   ```
   echo -e "\n. $(brew --prefix asdf)/libexec/asdf.sh" >> ~/.zshrc
   ```

   Then, restart your terminal or run `source ~/.zshrc`.

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
   - Install necessary tools (including GraalVM, Clojure, Node.js, nbb and Yarn)
   - Set up Git hooks
   - Install project dependencies
   - Perform any other necessary initialization steps

## Development

This project uses `just` as a command runner. To see all available commands, run:

```
just
```

Some useful commands include:

- `just run`: Run the CLI application (using nbb)
- `just run-clj`: Run the CLI application (using Clojure)
- `just test`: Run unit tests (using nbb)
- `just test-clj`: Run unit tests (using Clojure)
- `just watch`: Run unit tests and watch for changes (using nbb)
- `just fmt`: Format Clojure files
- `just sim`: Run a simulation with custom parameters (using nbb)
- `just sim-clj`: Run a simulation with custom parameters (using Clojure)
- `just repl`: Start a Clojure REPL
- `just build`: Build the project (creates uberjar and native image)

To update all tools to their latest versions:

```
just update-tools
```

## Usage

To start a new game:

```
just run
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

### Board Layout

The positions on the board are referenced in the program as shown below. The stars show the rosette squares:

![Royal Game of Ur Board](./docs/board.png)

### State Machine
The game is managed by a state machine that transitions between different states based on the player's actions and the game rules. The state machine diagram is shown below:

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
just test-clj
```

For continuous testing during development (using nbb):

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

## Troubleshooting

If you encounter any issues during setup or running the game, please check the following:

1. Ensure all prerequisites are correctly installed in the order specified.
2. Make sure your `PATH` includes the necessary directories for the installed tools.
3. If you're using a non-Mac system, you may need to adapt some of the commands or find alternative ways to install the required tools.

If problems persist, please open an issue on the GitHub repository.

## Contributing

This just a personal project for learning and fun, if you want to submit a pull
request maybe I'll look at it someday :-)

Feel free to fork the repository or steal any ideas from it.

## License

This project is open source and available under the [MIT License](LICENSE).
