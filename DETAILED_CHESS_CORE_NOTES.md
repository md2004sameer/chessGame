# Detailed Notes: Chess Core Design (Board, Piece, Game Interaction)

## 1. Learning objective
Use this project to understand how Object-Oriented Programming models a real system:
- A chessboard as state (`Board`, `Cell`)
- Chess pieces as behavior (`Piece` + concrete subclasses)
- Game rules as orchestration (`Game`, `GameStateChecker`)

The core teaching question:
How do objects collaborate to convert a player action ("move e2 to e4") into validated game state changes?

---

## 2. Core classes and responsibilities

### 2.1 `Board` (state container)
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Board.java`

Role:
- Owns 8x8 grid of `Cell` objects.
- Initializes starting piece positions.
- Gives access to individual cells.

Key code:
```java
Cell[][] cells;

public Board() {
    cells = new Cell[8][8];
    for (int i = 0; i < 8; i++) {
        for (int j = 0; j < 8; j++) {
            cells[i][j] = new Cell(i, j, null, (i + j) % 2 != 0);
        }
    }
}
```

What to teach:
- Composition: `Board` has many `Cell` objects.
- Encapsulation: board logic is hidden behind methods like `getCell`.

---

### 2.2 `Cell` (smallest board unit)
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Cell.java`

Role:
- Represents one square on board (`row`, `col`).
- Stores optional `Piece`.

Teaching point:
- A cell is a "holder" object; game logic should not be hardcoded inside each cell.

---

### 2.3 `Piece` (abstract behavior contract)
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Piece.java`

Role:
- Base class for all chess pieces.
- Holds shared fields (`isWhite`, `isKilled`).
- Delegates movement logic via strategy.

Key code:
```java
public abstract class Piece {
    MovementStrategy movementStrategy;
    boolean isWhite;
    boolean isKilled;

    public boolean canMove(Board board, Cell start, Cell end) {
        return movementStrategy.canMove(board, start, end);
    }

    public abstract String getDisplayChar();
}
```

What to teach:
- Abstraction: `Piece` defines common API.
- Polymorphism: any subclass can be used as `Piece`.
- Delegation: movement is delegated to strategy object.

---

### 2.4 Concrete pieces (`Pawn`, `Knight`, etc.)
Files:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/piece/Pawn.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/piece/Knight.java`
- etc.

Role:
- Bind a piece type to corresponding movement strategy.
- Provide display character for UI.

Teaching point:
- Concrete class specializes abstract class with specific behavior.

---

### 2.5 `MovementStrategy` (interface)
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/MovementStrategy.java`

```java
public interface MovementStrategy {
    public boolean canMove(Board board, Cell start, Cell end);
}
```

Implementations:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/movementStrategy/PawnMovementStrategy.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/movementStrategy/BishopMovementStrategy.java`
- etc.

What to teach:
- Strategy Pattern: each piece type has separate move algorithm.
- Open/Closed Principle: add new piece movement by adding class, not rewriting all logic.

---

### 2.6 `PieceFactory` (object creation)
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/PieceFactory.java`

Key code:
```java
public static Piece createPiece(String type, boolean isWhite) {
    switch (type.toLowerCase()) {
        case "pawn": return new Pawn(isWhite);
        case "rook": return new Rook(isWhite);
        ...
        default: throw new IllegalArgumentException("Unknown piece type: " + type);
    }
}
```

What to teach:
- Factory Pattern centralizes creation and avoids repeated `new` logic everywhere.

---

### 2.7 `Game` (orchestrator)
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Game.java`

Role:
- Holds current board + players + turn + status.
- Validates and applies moves.
- Handles advanced rules (castling, en passant, promotion).
- Updates game result state (win/stalemate).

This is the "brain" coordinating collaboration between board and pieces.

---

## 3. Interaction model: Board + Piece + Game

### 3.1 Mental model
When player requests move:
1. `Game` reads start/end cells from `Board`.
2. `Game` checks if move is legal contextually:
   - correct turn?
   - piece exists and belongs to current player?
   - king remains safe?
3. `Piece.canMove(...)` checks piece-specific geometry/rules through strategy.
4. If valid, `Game` mutates board state (`end.setPiece(...)`, `start.setPiece(null)`).
5. `Game` updates status, captures, special rules, and switches turn.

Important:
- `Board` stores state.
- `Piece` knows movement behavior.
- `Game` governs rule flow and state transitions.

---

## 4. Move flow in code (line-by-line concept)

### 4.1 Entry point: player move request
Inside `Game.makeMove(Move move, Player player)`:
```java
if (move == null || !isValidMove(move, player)) {
    return;
}
```

Teaching point:
- Guard clause prevents invalid state updates.

---

### 4.2 Get board squares and piece
```java
Cell start = move.getStart();
Cell end = move.getEnd();
Piece movingPiece = start.getPiece();
```

Teaching point:
- `Move` does not move piece itself; it only points from source to destination.

---

### 4.3 Validate ownership + turn + safety
`isValidMove(...)` checks:
- game status is ACTIVE
- current player is correct
- start/end not null
- piece belongs to player
- move does not expose king (`isMoveSafe`)
- piece-specific movement passes (`piece.canMove(...)`)

Key check:
```java
if (!isMoveSafe(start, end, player.isWhite())) {
    return false;
}

return piece.canMove(board, start, end);
```

Teaching point:
- Legality = global rule checks + local piece movement checks.

---

### 4.4 Apply state mutation
```java
end.setPiece(movingPiece);
start.setPiece(null);
```

Teaching point:
- This is the core board mutation.
- Everything else (capture logs, castling, en passant) is additional rule logic around it.

---

### 4.5 Special rules in orchestrator
`Game` handles:
- En passant capture
- Castling rook move
- Promotion via `PieceFactory`
- Game end detection (checkmate/stalemate)

Promotion example:
```java
if (promotion != null && !promotion.isEmpty()) {
    Piece moved = end.getPiece();
    if (moved instanceof Pawn) {
        end.setPiece(PieceFactory.createPiece(factoryType, moved.isWhite()));
    }
}
```

Teaching point:
- Special rules are centralized in one place (`Game`) to avoid duplication.

---

### 4.6 Turn switching
```java
private void switchTurn() {
    currentTurn = (currentTurn == whitePlayer) ? blackPlayer : whitePlayer;
}
```

Teaching point:
- Game state machine concept: one legal turn owner at a time.

---

## 5. Check/checkmate logic connection

File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/GameStateChecker.java`

`Game` delegates end-state logic to `GameStateChecker`:
```java
if (gameStateChecker.isCheckmate(!player.isWhite())) {
    gameStatus = player.isWhite() ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
    return;
}
```

Teaching point:
- Single Responsibility Principle: keep game state evaluation in dedicated class.

---

## 6. How UI/API connects to core chess classes

### 6.1 Controller receives move
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/controller/ChessController.java`

Example:
```java
Move move = (promotion == null) ? game.createMove(from, to) : game.createMove(from, to, promotion);
if (move != null) {
    game.makeMove(move, game.getCurrentTurn());
}
```

Teaching point:
- Controllers should translate request data to domain calls, not implement chess rules.

---

### 6.2 RoomService for multiplayer state
File:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/service/RoomService.java`

RoomService verifies player turn and applies game move:
```java
if (!game.getCurrentTurn().getName().equals(playerName)) return false;
Move move = (promotion == null) ? game.createMove(from, to) : game.createMove(from, to, promotion);
if (move == null) return false;
game.makeMove(move, game.getCurrentTurn());
```

Teaching point:
- Service layer enforces multiplayer rules around same core `Game`.

---

## 7. OOP concepts mapped directly to this project

1. Encapsulation:
- `Game` hides rule complexity behind `makeMove`.

2. Abstraction:
- `Piece` abstracts all concrete pieces.

3. Inheritance:
- `Pawn`, `Rook`, etc. inherit from `Piece`.

4. Polymorphism:
- `Piece.canMove` behaves differently by runtime type/strategy.

5. Composition:
- `Game` contains `Board`; `Board` contains `Cell`; `Cell` references `Piece`.

6. Strategy pattern:
- `MovementStrategy` implementations separate move algorithms.

7. Factory pattern:
- `PieceFactory` standardizes piece creation.

---

## 8. Suggested teaching demo (live in class)

### Demo A: trace one move
Use white pawn e2 -> e4.

Explain:
1. Convert `"e2"`, `"e4"` to cells (`createMove`).
2. Call `makeMove`.
3. Validate in `isValidMove`.
4. Validate pawn rule via strategy.
5. Update board cells.
6. Check mate/stalemate and switch turn.

### Demo B: invalid move
Try moving black piece on white turn.

Expected learning:
- Turn ownership check blocks move before board mutation.

### Demo C: promotion
Force pawn near last rank and pass promotion value (`q`, `r`, `b`, `n`).

Expected learning:
- Factory creates new piece class at runtime.

---

## 9. Design strengths and improvement ideas

### Strengths
- Strong OOP separation of concerns.
- Easy to extend piece behavior.
- Clear flow from request -> domain update -> response.

### Improvement ideas for students
1. Introduce immutable move/result objects for cleaner side-effect tracking.
2. Move board-to-DTO conversion into dedicated mapper class (currently duplicated).
3. Add persistence for rooms/games (currently in-memory).
4. Add richer validation feedback (why exactly move failed).

---

## 10. Practice tasks for students

1. Add a new endpoint to return move history from `gameLog`.
2. Write unit tests for one strategy class (e.g., knight movement edge cases).
3. Refactor common board serialization code into one reusable utility.
4. Add a "last move highlight" in UI using returned move metadata.
5. Add custom exception types for invalid move categories.

---

## 11. One-sentence summary for students
In this chess project, `Board` stores the position, `Piece` defines how each unit may move, and `Game` coordinates all rules so every move updates the board safely and legally.
