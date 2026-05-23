# ChessGame Teaching Notes (Student Learning Path)

## 1. Project goal
This project is a Spring Boot chess application with:
- Core chess logic (board, pieces, rules)
- Web UI (Thymeleaf + JavaScript)
- REST APIs
- Real-time gameplay with WebSocket/STOMP
- Optional AI move generation via Stockfish

Use this project to teach both:
- OOP design
- System connections between layers (UI -> Controller -> Service -> Domain -> Messaging)

## 2. Start from OOP basics (with this codebase)

### Class and object
- `Game`, `Board`, `Cell`, `Player`, `Move` are domain classes.
- A `Game` object owns game state (players, board, turn, status).

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Game.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Board.java`

### Encapsulation
- `Game.makeMove(...)` controls move validation + state updates.
- Data is exposed using getters (`getBoard()`, `getCurrentTurn()`, etc.).
- Students should learn: game rules are centralized, not scattered.

### Inheritance
- `Piece` is abstract.
- Concrete pieces (`Pawn`, `Rook`, `Knight`, `Bishop`, `Queen`, `King`) extend `Piece`.

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/Piece.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/piece`

### Polymorphism
- Code handles pieces via `Piece` type but behavior changes by actual piece.
- Example: `piece.canMove(...)` works for all pieces, but each piece has different movement.

### Interface + Strategy Pattern
- `MovementStrategy` is an interface.
- Each piece uses a movement strategy implementation.
- This is a clean example of replacing `if-else` chains with composable behavior.

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/MovementStrategy.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/movementStrategy`

### Factory Pattern
- `PieceFactory.createPiece(type, isWhite)` creates correct piece objects.
- Useful to teach object creation centralization.

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/entity/PieceFactory.java`

## 3. “Connection” topics to teach

### A. Object-level connection (composition)
- `Game` contains `Board`, `Player`, move log, captured lists.
- `Board` contains `Cell[][]`.
- `Cell` contains `Piece`.

Teach as: "has-a" relationships.

### B. Layer connection (Spring architecture)
- `Controller` handles HTTP/WS requests.
- `Service` handles room management and messaging.
- `Entity` layer holds chess logic.

Main flow:
1. UI sends request
2. Controller receives request
3. Controller calls service
4. Service updates `Game`
5. Response/state sent back to clients

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/controller/ChessController.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/controller/WsGameController.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/service/RoomService.java`

### C. Dependency Injection connection
- Spring injects dependencies through constructors:
  - `ChessController(StockfishService, RoomService, MessagingService)`
  - `WsGameController(RoomService, MessagingService)`
- Teach why DI helps testing and loose coupling.

### D. Client-server connection (HTTP + WebSocket)
- REST examples:
  - `POST /rooms` create room
  - `POST /room/{roomId}/join`
  - `POST /room/{roomId}/move`
  - `GET /room/{roomId}/state`
- WebSocket/STOMP:
  - Endpoint: `/ws`
  - App destination prefix: `/app`
  - Topic prefix: `/topic`

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/config/WebSocketConfig.java`
- `/Users/sameerog/Documents/chessGame-2/src/main/resources/templates/game.html`

### E. External process connection
- `StockfishService` starts Stockfish process and communicates using UCI commands.
- Great example of Java app connecting to an external engine.

Reference:
- `/Users/sameerog/Documents/chessGame-2/src/main/java/com/example/ChessGame/service/StockfishService.java`

## 4. Suggested classroom sequence (6 sessions)

### Session 1: OOP foundations in this project
- Explain classes, objects, encapsulation.
- Walk through `Game`, `Board`, `Cell`, `Move`, `Player`.
- Task: print board state in readable format.

### Session 2: Abstraction and polymorphism
- Explain abstract class `Piece`.
- Explain `canMove(...)` polymorphism.
- Task: add one debug method to show piece type and allowed moves.

### Session 3: Strategy + Factory patterns
- Explain `MovementStrategy` and why it is better than giant conditionals.
- Explain `PieceFactory`.
- Task: add a custom piece prototype (for learning only).

### Session 4: MVC and REST connection
- Explain Spring controllers and model binding.
- Follow `/room/{roomId}/join` and `/room/{roomId}/move`.
- Task: add one new REST endpoint (e.g., move history API).

### Session 5: Real-time connection with WebSocket/STOMP
- Explain pub/sub model.
- Show how client subscribes and receives board updates.
- Task: broadcast and render last move message.

### Session 6: Testing + refactoring
- Run tests and inspect examples.
- Show unit vs integration tests.
- Task: write test for one new edge case (e.g., invalid join name).

Reference tests:
- `/Users/sameerog/Documents/chessGame-2/src/test/java/com/example/ChessGame`

## 5. Student exercises (progressive)

1. Add a `draw by agreement` feature (REST + UI button + state update).
2. Add move history endpoint returning algebraic notation text.
3. Improve `validMoves` endpoint to filter out moves that leave king in check.
4. Add DTO mapper utility to reduce repeated board-to-response conversion code.
5. Add error handling standard response format for REST and WS.

## 6. Teaching checkpoints (what students should understand)

By the end, students should be able to explain:
1. Why `Piece` is abstract and how polymorphism works here.
2. How Strategy and Factory patterns are used in real code.
3. How a move request travels from browser to domain model and back.
4. Difference between REST request-response and WebSocket push.
5. Why services are separated from controllers.

## 7. Current project notes (for mentor)

- Room/game data is in-memory (`ConcurrentHashMap`), not persisted to a database.
- This project is strong for OOP + connection architecture learning.
- Good next learning upgrade: add persistence (JPA/H2/Postgres) as an advanced module.
