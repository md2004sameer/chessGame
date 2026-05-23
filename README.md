ChessGame
========

Project overview
----------------

ChessGame is a Spring Boot-based multiplayer chess server and web client. It offers REST and WebSocket endpoints to create/join game rooms and play chess in real time. The application models core chess concepts (board, pieces, moves, movement strategies), supports human vs human play over WebSocket, and integrates with a Stockfish service for AI moves.

Key features
------------

- Real-time play using STOMP over WebSocket.
- Room management and matchmaking via REST and WebSocket controllers.
- Clear domain model: `Game`, `Board`, `Piece`, `Move`, and pluggable `MovementStrategy` implementations.
- Unit and integration tests covering game rules, REST endpoints, and WebSocket flows.
- Optional Stockfish integration to provide an AI opponent (`StockfishService`).

Architecture and packages
-------------------------

The source is organized under `src/main/java/com/example/ChessGame` with the following top-level packages:

- `config` — Spring configuration (WebSocket setup).
- `controller` — REST and WebSocket controllers and event listeners.
- `entity` — Domain model: board, cells, pieces, movement strategies, and game logic.
- `service` — Application services: messaging, room management, and Stockfish integration.

Important files
---------------

- [src/main/java/com/example/ChessGame/ChessGameApplication.java](src/main/java/com/example/ChessGame/ChessGameApplication.java) : Spring Boot application entry.
- [src/main/java/com/example/ChessGame/config/WebSocketConfig.java](src/main/java/com/example/ChessGame/config/WebSocketConfig.java) : STOMP/WebSocket configuration.
- [src/main/java/com/example/ChessGame/controller/ChessController.java](src/main/java/com/example/ChessGame/controller/ChessController.java) : REST endpoints for room and move operations.
- [src/main/java/com/example/ChessGame/controller/WsGameController.java](src/main/java/com/example/ChessGame/controller/WsGameController.java) : WebSocket endpoints for real-time play.
- [src/main/java/com/example/ChessGame/service/RoomService.java](src/main/java/com/example/ChessGame/service/RoomService.java) : Handles room lifecycle and game creation.
- [src/main/java/com/example/ChessGame/service/MessagingService.java](src/main/java/com/example/ChessGame/service/MessagingService.java) : Encapsulates STOMP messaging operations.
- [src/main/java/com/example/ChessGame/service/StockfishService.java](src/main/java/com/example/ChessGame/service/StockfishService.java) : Optional AI integration point.
- [src/main/java/com/example/ChessGame/entity/Game.java](src/main/java/com/example/ChessGame/entity/Game.java) : Central game state and move application.
- `entity/movementStrategy/*` and `entity/piece/*` : Movement rules and concrete piece implementations (King, Queen, Rook, Bishop, Knight, Pawn).

Domain model summary
--------------------

- `Game` — Holds players, `Board`, move history, and enforces turn order and status (`GameStatus`).
- `Board` and `Cell` — Grid of `Cell` objects containing `Piece` references.
- `Piece` and `PieceFactory` — Piece instances with color and movement capabilities.
- `MovementStrategy` implementations — Encapsulate movement rules per piece type (e.g., `BishopMovementStrategy`, `PawnMovementStrategy`).
- `GameStateChecker` — Evaluates check/checkmate/stalemate conditions.

Web UI and static assets
------------------------

Client templates and static JS are in `src/main/resources/templates` and `src/main/resources/static/js`. The Web UI uses STOMP over WebSocket (client JS includes `stomp.umd.min.js`) and template pages: `index.html`, `start.html`, and `game.html`.

Endpoints and messaging
----------------------

REST endpoints (examples):
- Create/join room and move via `ChessController` (see controller sources).

WebSocket (STOMP) topics and destinations (configured in `WebSocketConfig`):
- Clients subscribe to topics for room updates and game moves.
- `WsGameController` publishes move results and game state updates through `MessagingService`.

Stockfish / AI
--------------

`StockfishService` is an integration point for external engine moves. The service encapsulates communication with Stockfish (or a placeholder) and returns engine-calculated best moves to the `RoomService` when AI play is enabled.

Testing
-------

Tests are under `src/test/java/com/example/ChessGame`. The test suite includes:

- Unit tests for game logic and FEN handling (e.g., `GameFenAndPromotionTests`).
- Integration tests for REST and WebSocket flows (e.g., `MoveEndpointIntegrationTest`, `WsGameIntegrationTest`, `TwoClientsE2ETest`).
- Controller tests and fallback/static tests for the web UI (`StaticFallbackControllerTest`).

Run tests with Gradle:

```bash
./gradlew test
```

Build and run
-------------

Build and run locally using the Gradle wrapper:

```bash
./gradlew clean build
./gradlew bootRun
```

Application configuration
-------------------------

`src/main/resources/application.properties` contains runtime properties such as server port and other Spring Boot configs.

Development notes and suggestions
--------------------------------

- Game rules are implemented with strategy objects. Extending rules (e.g., custom variants) can be done by adding new `MovementStrategy` implementations and registering them in `PieceFactory`.
- Stockfish integration is isolated — you can replace or mock `StockfishService` for testing.
- Tests include both unit and integration coverage — run them regularly during development.

Contributing
------------

1. Fork the repo and create a topic branch.
2. Run tests locally with `./gradlew test`.
3. Open a pull request with focused changes and tests.

License
-------

This repository does not include a license file. Add one if you intend to make the project open source.

Contact / Questions
-------------------

If you want, I can:

- Add more detailed API documentation listing exact REST paths and STOMP destinations after scanning controller source files.
- Run tests and report results.
- Add a minimal README badge or CI instructions.

Created README for the project.
