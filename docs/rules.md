# Game Rules: The Royal Game of Ur

The Royal Game of Ur is an ancient board game that was played in Mesopotamia over 4,500 years ago. This document outlines the rules as implemented in our digital version.

## The Board

The game is played on a board with 20 squares arranged in three rows:

```
    1 2 3 4 5 6 7 8
  ┌──────────────────┐
A │✧ · · · ✧ · · ✧│
B │· · · · ✧ · · ·│
C │✧ · · · ✧ · · ✧│
  └──────────────────┘
```

- The ✧ symbols represent "rosette" squares, which grant special privileges.
- Each player has their own path through the board, sharing the middle row.

## Setup

- Each player starts with 7 pieces off the board.
- Players decide randomly who goes first.

## Gameplay

1. **Rolling the Dice**: Players roll four special dice to determine their move. In our digital version, this is simulated to give a result of 0-4.

2. **Moving Pieces**: 
   - Players can either bring a new piece onto the board or move an existing piece.
   - Pieces always move in a specific direction: up the player's outside row, across the middle row from left to right, and down the opponent's outside row.

3. **Special Rules**:
   - If a player lands on a rosette, they get an extra turn.
   - If a player lands on a square occupied by an opponent's piece, the opponent's piece is captured and must start over.
   - Players cannot land on squares occupied by their own pieces.
   - The middle rosette is safe - players cannot capture pieces on this square.

4. **Leaving the Board**: 
   - Pieces must leave the board on an exact roll.
   - When a piece leaves the board, it's out of play for the rest of the game.

## Winning the Game

The first player to move all seven of their pieces off the board wins the game.

## Strategy Tips

- Try to get pieces onto the rosettes for extra turns.
- Be careful about leaving single pieces vulnerable to capture.
- Sometimes it's worth not moving to prevent your opponent from capturing your pieces.
- Towards the end of the game, managing your dice rolls becomes crucial to move pieces off the board.
