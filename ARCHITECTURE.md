# Chess Game - System Architecture

## Overview

The Chess Game application is built using a layered architecture with Spring Boot, featuring multiplayer room-based gameplay with both HTTP and WebSocket support.

---

## Architecture Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (HTML/Thymeleaf Templates)            │
│  - start.html (room selection)         │
│  - game.html (game board)              │
└─────────────────────────────────────────┘
                    ▲
                    │ HTTP/WebSocket
                    ▼
┌─────────────────────────────────────────┐
│         API Layer (Controllers)         │
│  - ChessController (Legacy)            │
│  - RoomRestController (REST)           │
│  - WsGameController (WebSocket)        │
└─────────────────────────────────────────┘
                    ▲
                    │ Business Logic
                    ▼
┌─────────────────────────────────────────┐
│         Service Layer                   │
│  - RoomService (Room Management)       │
│  - StockfishService (AI Engine)        │
│  - MessagingService (WebSocket Broadcast) │
└─────────────────────────────────────────┘
                    ▲
                    │ Domain Models
                    ▼
┌─────────────────────────────────────────┐
│         Entity/Model Layer              │
│  - Game (Board state, moves)           │
│  - Room (Room state, players)          │
│  - Player (Player info)                │
│  - Board/Cell/Piece (Chess entities)   │
│  - Move (Move representation)          │
└─────────────────────────────────────────┘
```

---

## Key Components

### 1. Controllers Layer

#### ChessController (`ChessController.java`)
- **Purpose**: Handles single-player game mode and legacy HTTP endpoints
- **Endpoints**:
  - `GET /` - Start page
  - `POST /start` - Create local game
  - `GET /game` - Show game board
  - `POST /rooms` - Create multiplayer room (redirect)
  - `GET /room/{roomId}` - Show room view
- **Session Management**: Uses HttpSession for player state
- **Status**: Legacy, maintained for backwards compatibility

#### RoomRestController (`RoomRestController.java`)
- **Purpose**: RESTful API for room management
- **Endpoints**:
  - `POST /api/rooms` - Create room
  - `POST /api/rooms/{roomId}/join` - Join room
  - `GET /api/rooms` - List rooms
  - `GET /api/rooms/{roomId}` - Get room info
  - `GET /api/rooms/{roomId}/players/{playerName}` - Check player presence
- **Response Format**: JSON
- **Error Handling**: Comprehensive error responses
- **CORS**: Enabled for cross-origin requests

#### WsGameController (`WsGameController.java`)
- **Purpose**: WebSocket message handling for real-time gameplay
- **Endpoints**:
  - `@MessageMapping(/rooms/join)` - WebSocket join
  - `@MessageMapping(/rooms/{roomId}/move)` - Send move
- **Message Flow**: JSON messages to/from clients
- **Broadcasting**: Uses MessagingService for real-time updates

### 2. Service Layer

#### RoomService (`RoomService.java`)
- **Purpose**: Core business logic for room management
- **Key Methods**:
  - `createRoom(String creatorName)` - Create new room
  - `joinRoom(String roomId, String playerName)` - Join existing room
  - `applyMove(String roomId, String from, String to, String promotion, String playerName)` - Apply game move
  - `getRoom(String roomId)` - Retrieve room
  - `getActiveRooms()` - List all active rooms
  - `isPlayerInRoom(String roomId, String playerName)` - Check player membership
- **Thread Safety**: Synchronized methods, ConcurrentHashMap storage
- **Validation**: Player names, room IDs, move legality

#### StockfishService (`StockfishService.java`)
- **Purpose**: AI opponent using Stockfish engine
- **Functions**:
  - Generate AI moves
  - Evaluate positions
  - Set difficulty levels
- **Integration**: Used in single-player mode

#### MessagingService (`MessagingService.java`)
- **Purpose**: WebSocket message broadcasting
- **Functions**:
  - Send messages to room topics
  - Handle subscription events
  - Broadcast game state updates

### 3. Entity Layer

#### Game (`Game.java`)
- **Represents**: Complete chess game state
- **Fields**:
  - `Board board` - 8x8 chess board
  - `Player whitePlayer` - White player
  - `Player blackPlayer` - Black player
  - `Player currentTurn` - Current turn
  - `GameStatus gameStatus` - Game state
  - `ArrayList<Move> gameLog` - Move history
  - `Map<Piece> capturedWhite/Black` - Captured pieces
- **Key Methods**:
  - `makeMove(Move move, Player player)` - Execute move
  - `isValidMove(Move move, Player player)` - Check move legality
  - `getFen()` - Get FEN notation
  - `createMove(String from, String to, [String promotion])` - Create move
- **Responsibilities**:
  - Enforce chess rules
  - Manage piece movement
  - Detect checkmate, stalemate
  - Track game state

#### Room (`Room.java`)
- **Represents**: A multiplayer game room
- **Fields**:
  - `String roomId` - Unique room identifier
  - `String creatorName` - Room creator
  - `String joinerName` - Second player (nullable)
  - `Game game` - Associated game
  - `RoomStatus status` - Room state
  - `long createdAt/updatedAt` - Timestamps
- **Status Enum**:
  - `WAITING` - Awaiting opponent
  - `ACTIVE` - Game in progress
  - `COMPLETED` - Game finished
  - `ABANDONED` - Room abandoned
- **Key Methods**:
  - `isFull()` - Check if both slots filled
  - `hasPlayer(String playerName)` - Check membership
  - `setStatus(RoomStatus status)` - Update status

#### Player (`Player.java`)
- **Represents**: Individual player
- **Fields**:
  - `String name` - Player name
  - `boolean isWhite` - Color assignment
- **Responsibilities**: Player identification

#### Board/Cell/Piece (`Board.java`, `Cell.java`, `Piece.java`)
- **Represents**: Chess board state and pieces
- **Responsibilities**:
  - Maintain piece positions
  - Support piece movement
  - Enforce board constraints

#### Move (`Move.java`)
- **Represents**: Individual chess move
- **Fields**:
  - `Cell start` - Source square
  - `Cell end` - Destination square
  - `String promotion` - Pawn promotion piece (q,r,b,n)

### 4. Data Transfer Objects (DTOs)

#### RoomRequest
```java
{
  "playerName": "string"
}
```

#### RoomResponse
```java
{
  "roomId": "string",
  "creatorName": "string",
  "joinedPlayerName": "string or null",
  "status": "enum",
  "playerCount": "int",
  "createdAt": "long"
}
```

#### ErrorResponse
```java
{
  "error": "ERROR_CODE",
  "message": "Human-readable message",
  "status": "int",
  "timestamp": "long"
}
```

---

## Data Flow

### Room Creation Flow
```
1. User submits form with name
   ↓
2. POST /api/rooms → RoomRestController.createRoom()
   ↓
3. RoomService.createRoom() validates name
   ↓
4. Generate unique room ID
   ↓
5. Create Room entity with initial Game
   ↓
6. Store in ConcurrentHashMap
   ↓
7. Return RoomResponse (201 Created)
   ↓
8. Client navigates to room page
```

### Room Joining Flow
```
1. User enters room ID and name
   ↓
2. POST /api/rooms/{roomId}/join → RoomRestController.joinRoom()
   ↓
3. RoomService.joinRoom() validates
   ↓
4. Check room exists and has free slot
   ↓
5. Update Room.joinerName
   ↓
6. Set Room.status = ACTIVE
   ↓
7. Update Game.blackPlayer
   ↓
8. Return RoomResponse with updated state
   ↓
9. WebSocket broadcasts new game state
```

### Move Flow
```
1. Player clicks chess board cells
   ↓
2. JavaScript validates move locally
   ↓
3. POST /move or WebSocket /app/rooms/{roomId}/move
   ↓
4. RoomService.applyMove() validates
   ↓
5. Game.makeMove() executes move
   ↓
6. Update Game.gameStatus (check, checkmate, etc)
   ↓
7. MessagingService broadcasts to room topic
   ↓
8. Both clients receive update
   ↓
9. UI updates board state
```

---

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend | HTML5, CSS3, JavaScript, Thymeleaf | UI and templating |
| HTTP API | Spring MVC, REST | RESTful endpoints |
| WebSocket | Spring WebSocket, STOMP | Real-time communication |
| Backend | Java 21, Spring Boot 3.5.14 | Application framework |
| Storage | In-memory (ConcurrentHashMap) | Room/game state |
| Build | Gradle 8.x | Build automation |
| AI | Stockfish | Chess engine |

---

## Design Patterns

### 1. Service Layer Pattern
- Business logic isolated in Service classes
- Controllers delegate to services
- Enables testing and reusability

### 2. Repository Pattern
- ConcurrentHashMap acts as in-memory repository
- Can be replaced with database later
- Thread-safe operations

### 3. Singleton Pattern
- RoomService, StockfishService as Singletons
- Spring manages lifecycle
- Single instance per application

### 4. Observer Pattern (WebSocket)
- STOMP topic subscriptions
- Real-time event broadcasting
- Decoupled sender/receiver

### 5. Factory Pattern
- Room generation with ID creation
- Move creation with promotion handling
- Player instantiation

### 6. State Pattern
- Room statuses (WAITING, ACTIVE, COMPLETED)
- Game status tracking
- Clear state transitions

---

## Threading Model

### Concurrency Considerations

1. **Room Storage**
   - `ConcurrentHashMap<String, Room>` for thread-safe access
   - No need for external locks for read operations

2. **Room Operations**
   - `synchronized` methods for write operations:
     - `joinRoom()` - Synchronized to prevent race conditions
     - `applyMove()` - Synchronized to ensure move atomicity
   
3. **WebSocket Messages**
   - STOMP broker handles message queuing
   - Thread pool processes messages
   - No blocking operations

### Thread Safety Strategy
```
Concurrent Reads          Synchronized Writes
    ↓                           ↓
  Safe              Creates bottleneck
    ↓                           ↓
Use ConcurrentHashMap   Use minimal synchronized blocks
```

---

## Scalability Considerations

### Current Limitations (In-Memory)
- Rooms stored in application memory
- No persistence across restarts
- Limited to single JVM instance

### Future Improvements
1. **Database Integration**
   - Replace ConcurrentHashMap with database
   - Persist rooms and game history
   - Enable multi-server deployment

2. **Redis/Cache**
   - Distributed caching layer
   - Session management
   - Rate limiting

3. **Microservices**
   - Separate room service
   - Separate game service
   - Separate AI service

4. **Load Balancing**
   - Multiple instances with shared database
   - WebSocket sticky sessions
   - API gateway

---

## Error Handling Strategy

### Exception Hierarchy
```
Exception
  ├── IllegalArgumentException
  │   ├── Invalid room ID
  │   ├── Invalid player name
  │   └── Room full
  └── Runtime errors
      ├── Null pointer (defensive programming)
      └── Resource exhaustion
```

### Error Response Format
```json
{
  "error": "ENUM_CODE",
  "message": "Human-readable message",
  "status": 400,
  "timestamp": 1716545400000
}
```

### Logging Strategy
- INFO: Room creation, joining events
- WARN: Validation failures, rejections
- ERROR: Unexpected exceptions
- DEBUG: Move details, state changes

---

## Security Model

### Input Validation
1. **Player Names**
   - Length: 1-50 characters
   - Allowed: alphanumeric, spaces, -, _
   - Trimmed and validated

2. **Room IDs**
   - Numeric (100000-999999) or UUID
   - Validated on lookup
   - Collision prevention

3. **Chess Moves**
   - Validated by Game entity
   - Player turn verification
   - Legality checking

### Current Limitations
- No authentication (assumes trusted players)
- No authorization (all players see all rooms)
- CORS enabled for development

### Recommended for Production
- JWT authentication
- Role-based access control
- Rate limiting
- HTTPS enforcement
- CORS restrictions

---

## Testing Strategy

### Unit Tests
- Game rule validation
- Move legality
- Room state transitions
- Player validation

### Integration Tests
- Full room lifecycle
- Multi-player scenarios
- WebSocket communication
- REST API responses

### Test Data
- Various board positions
- Checkmate scenarios
- Stalemate scenarios
- Invalid moves

---

## Configuration Management

### Application Properties
```properties
server.port=8080
server.servlet.context-path=/
spring.application.name=ChessGame
```

### Environmental Configuration
- Development: In-memory storage, CORS enabled
- Production: Database backend, CORS restricted

---

## Monitoring & Observability

### Metrics to Track
- Active rooms count
- Move latency
- Error rates
- WebSocket connection count

### Logging Points
- Room creation/joining
- Move application
- Game completion
- Error conditions

---

## Deployment Architecture

### Current (Development)
```
┌──────────────────┐
│  Spring Boot App │
│  - Controllers   │
│  - Services      │
│  - In-Memory DB  │
│  - WebSocket     │
└──────────────────┘
```

### Recommended (Production)
```
┌────────────────┐
│ API Gateway    │
└────────┬───────┘
         │
    ┌────┴─────┐
    ▼          ▼
┌────────┐  ┌────────┐
│ App 1  │  │ App 2  │
│(Spring)│  │(Spring)│
└────┬───┘  └───┬────┘
     └────┬────┘
          ▼
     ┌─────────┐
     │Database │
     │(MySQL)  │
     └─────────┘
```

---

## Development Workflow

### Adding a New Feature
1. Define entity changes (if needed)
2. Update service layer
3. Create REST endpoint (if needed)
4. Update WebSocket handler (if needed)
5. Update templates/frontend
6. Add tests
7. Test manually
8. Document in API docs

### Code Organization
```
src/main/java/com/example/ChessGame/
├── ChessGameApplication.java
├── config/
│   └── WebSocketConfig.java
├── controller/
│   ├── ChessController.java
│   ├── RoomRestController.java
│   ├── WsGameController.java
│   └── dto/
│       ├── RoomRequest.java
│       ├── RoomResponse.java
│       ├── JoinRoomRequest.java
│       └── ErrorResponse.java
├── entity/
│   ├── Game.java
│   ├── Room.java
│   ├── Player.java
│   ├── Board.java
│   ├── Cell.java
│   ├── Move.java
│   └── piece/
│       ├── Piece.java
│       ├── Pawn.java
│       ├── Rook.java
│       └── ... (other pieces)
├── service/
│   ├── RoomService.java
│   ├── StockfishService.java
│   ├── MessagingService.java
│   └── GameStateChecker.java
└── listener/
    ├── WebSocketEventListener.java
    └── SubscriptionEventListener.java
```

---

## Future Architecture Improvements

1. **Event-Driven Architecture**
   - Replace direct method calls with events
   - Event sourcing for game history
   - CQRS for read/write separation

2. **GraphQL API**
   - Alternative to REST for flexibility
   - Real-time subscriptions
   - Better client control

3. **Microservices**
   - Room service (separate)
   - Game engine service (separate)
   - AI service (separate)
   - User service (separate)

4. **Message Queue**
   - RabbitMQ or Kafka
   - Async move processing
   - Game notifications
   - Audit trail

5. **Caching Layer**
   - Redis for room lookups
   - Cache game positions
   - Session management

---

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [WebSocket with STOMP](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Chess Rules](https://en.wikipedia.org/wiki/Rules_of_chess)
- [Stockfish](https://stockfishchess.org/)
- [REST API Best Practices](https://restfulapi.net/)
