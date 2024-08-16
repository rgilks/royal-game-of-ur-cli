# Royal Game of Ur

![Royal Game of Ur Console](./docs/screen.png)

## Table of Contents
- [Royal Game of Ur](#royal-game-of-ur)
  - [Table of Contents](#table-of-contents)
  - [Introduction](#introduction)
  - [Features](#features)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
  - [Development](#development)
  - [Usage](#usage)
  - [Game Rules](#game-rules)
  - [Project Structure](#project-structure)
  - [Testing](#testing)
  - [License](#license)

## Introduction

The Royal Game of Ur, also known as the Game of Twenty Squares, is one of the oldest known board games, dating back to around 2600 BCE. This project implements a digital version of this ancient game, complete with a command-line interface and a robust state machine design.

## Features

- Faithful implementation of the Royal Game of Ur rules
- Command-line interface for easy gameplay
- Robust state machine design for game logic
- AI opponent with configurable strategies
- Comprehensive test suite
- Colorized output for enhanced user experience

## Getting Started

### Prerequisites

This project uses version management to ensure consistent development environments. You can use tools like `asdf`, `mise`, or any other version manager of your choice. The required tool versions are specified in the `.tool-versions` file.

Ensure you have a compatible version manager installed on your system.

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/royal-game-of-ur.git
   cd royal-game-of-ur
   ```

2. Install the required tool versions:
   If you're using `asdf`, `mise`, or a compatible version manager, it should automatically pick up the versions from `.tool-versions`. If not, manually install the specified versions of each tool.

3. Initialize the project:
   ```
   just init
   ```

   This command will:
   - Install the required tool versions (if using `asdf`)
   - Set up necessary tools (including Graphviz and nbb)
   - Set up Git hooks
   - Perform any other necessary initialization steps

## Development

This project uses `just` as a command runner. To see all available commands, run:

```
just
```

Some useful commands include:

- `just run`: Run the application
- `just watch`: Run unit tests and watch for changes
- `just test`: Run unit tests
- `just fmt`: Format Clojure files
- `just concat`: Concatenate all project files
- `just state-diagram`: Generate the state diagram

To update all tools to their latest versions (if using `asdf`):

```
just update-tools
```

## Usage

To start a new game:

```
just run
```

This will launch the game in your terminal. Follow the on-screen instructions to play.

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

## License

This project is open source and available under the [MIT License](LICENSE).
