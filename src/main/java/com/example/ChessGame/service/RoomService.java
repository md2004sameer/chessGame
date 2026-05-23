package com.example.ChessGame.service;

import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Player;
import com.example.ChessGame.entity.Move;
import com.example.ChessGame.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class RoomService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private static final long WAITING_ROOM_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long COMPLETED_ROOM_EXPIRATION_MS = TimeUnit.HOURS.toMillis(6);
    private static final long ABANDONED_ROOM_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(60);
    private static final long ACTIVE_ROOM_STALE_MS = TimeUnit.HOURS.toMillis(24);

    private final ScheduledExecutorService cleanupExecutor = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r, "room-cleanup-thread");
        thread.setDaemon(true);
        return thread;
    });

    public RoomService() {
        cleanupExecutor.scheduleAtFixedRate(this::removeExpiredRooms, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Create a new game room
     */
    public String createRoom(String creatorName) {
        String name = validatePlayerName(creatorName);
        
        String id = generateRoomId();
        Room room = new Room(id, name);
        String creatorToken = createPlayerToken();
        room.setCreatorToken(creatorToken);
        
        // Create initial game with placeholder
        Player white = new Player(name, true);
        Player placeholderBlack = new Player("Waiting for opponent...", false);
        Game game = new Game(white, placeholderBlack);
        game.startGame();
        room.setGame(game);
        
        rooms.put(id, room);
        logger.info("Room created: {} by player: {}", id, name);
        return id;
    }

    /**
     * Join an existing room
     */
    public static class JoinResult {
        private final String role;
        private final String playerToken;

        public JoinResult(String role, String playerToken) {
            this.role = role;
            this.playerToken = playerToken;
        }

        public String getRole() {
            return role;
        }

        public String getPlayerToken() {
            return playerToken;
        }
    }

    public synchronized String joinRoom(String roomId, String playerName) throws IllegalArgumentException {
        return joinRoom(roomId, playerName, null).getRole();
    }

    public synchronized JoinResult joinRoom(String roomId, String playerName, String playerToken) throws IllegalArgumentException {
        logger.info("Join room requested: roomId={}, playerName={}", roomId, playerName);
        
        String name = validatePlayerName(playerName);
        Room room = rooms.get(roomId);
        
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        if (name.equals(room.getCreatorName())) {
            if (!room.isCreatorAuthorized(name, playerToken)) {
                throw new IllegalArgumentException("Invalid token for creator");
            }
            logger.info("Creator {} rejoined room {}", name, roomId);
            return new JoinResult("white", room.getCreatorToken());
        }

        if (name.equals(room.getJoinerName())) {
            if (!room.isJoinerAuthorized(name, playerToken)) {
                throw new IllegalArgumentException("Invalid token for joiner");
            }
            logger.info("Player {} rejoined room {} as black", name, roomId);
            return new JoinResult("black", room.getJoinerToken());
        }

        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new IllegalArgumentException("Room is not available (status: " + room.getStatus() + ")");
        }

        if (room.isFull()) {
            throw new IllegalArgumentException("Room is full");
        }

        // Set joiner and update game
        room.setJoinerName(name);
        String joinerToken = createPlayerToken();
        room.setJoinerToken(joinerToken);
        room.setStatus(Room.RoomStatus.ACTIVE);
        
        if (room.getGame() != null) {
            room.getGame().setBlackPlayer(new Player(name, false));
        }
        
        logger.info("Player {} joined room {} as black", name, roomId);
        return new JoinResult("black", joinerToken);
    }

    /**
     * Get a room by ID
     */
    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * Get game from room
     */
    public Game getGame(String roomId) {
        Room room = rooms.get(roomId);
        return room != null ? room.getGame() : null;
    }

    /**
     * Get all active rooms (for listing/discovery)
     */
    public List<Room> getActiveRooms() {
        return new ArrayList<>(rooms.values());
    }

    /**
     * Apply a move in a room's game
     */
    public synchronized boolean applyMove(String roomId, String from, String to, String promotion, String playerName) {
        return applyMove(roomId, from, to, promotion, playerName, null);
    }

    public synchronized boolean applyMove(String roomId, String from, String to, String promotion, String playerName, String playerToken) {
        Room room = rooms.get(roomId);
        if (room == null || room.getGame() == null) {
            return false;
        }

        if (room.getStatus() != Room.RoomStatus.ACTIVE || !room.isFull()) {
            logger.warn("Move rejected before room is active: roomId={}, playerName={}, status={}",
                    roomId, playerName, room.getStatus());
            return false;
        }

        if (!room.isPlayerAuthorized(playerName, playerToken)) {
            logger.warn("Invalid move token for player {} in room {}", playerName, roomId);
            return false;
        }

        Game game = room.getGame();
        
        // Verify it's the player's turn
        if (!game.getCurrentTurn().getName().equals(playerName)) {
            logger.warn("Invalid move attempt: {} tried to move but it's {} turn in room {}", 
                       playerName, game.getCurrentTurn().getName(), roomId);
            return false;
        }

        // Create and apply the move
        Move move = (promotion == null) ? 
                   game.createMove(from, to) : 
                   game.createMove(from, to, promotion);
        
        if (move == null) {
            return false;
        }

        boolean moved = game.makeMove(move, game.getCurrentTurn());
        if (!moved) {
            return false;
        }

        room.setLastMove(from, to, promotion);
        room.setUpdatedAt(System.currentTimeMillis());
        return true;
    }

    /**
     * Check if player is in a room
     */
    public boolean isPlayerInRoom(String roomId, String playerName) {
        Room room = rooms.get(roomId);
        return room != null && room.hasPlayer(playerName);
    }

    /**
     * Get room info for debugging
     */
    public Map<String, Object> getRoomInfo(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("roomId", room.getRoomId());
        info.put("creator", room.getCreatorName());
        info.put("joiner", room.getJoinerName());
        info.put("status", room.getStatus());
        info.put("playerCount", room.isFull() ? 2 : 1);
        info.put("createdAt", room.getCreatedAt());
        info.put("updatedAt", room.getUpdatedAt());
        return info;
    }

    // ========== Helper Methods ==========

    /**
     * Validate and sanitize player name
     */
    private String validatePlayerName(String name) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }
        
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("Player name too long (max 50 characters)");
        }
        
        if (!trimmed.matches("[\\p{L}0-9 _\\-]{1,50}")) {
            throw new IllegalArgumentException("Invalid characters in name. Allowed: letters, numbers, spaces, '-', '_'");
        }
        
        return trimmed;
    }

    /**
     * Generate a unique room ID
     */
    private String generateRoomId() {
        final int MAX_ATTEMPTS = 10;
        
        // Try numeric IDs first
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = String.format("%06d", 
                java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000));
            if (!rooms.containsKey(candidate)) {
                return candidate;
            }
        }
        
        // Fallback to UUID if numeric IDs exhausted
        return java.util.UUID.randomUUID().toString();
    }

    private String createPlayerToken() {
        return java.util.UUID.randomUUID().toString();
    }

    private void removeExpiredRooms() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Room> entry : rooms.entrySet()) {
            Room room = entry.getValue();
            if (isExpired(room, now)) {
                rooms.remove(entry.getKey());
                logger.info("Expired room removed: {} (status={}, updatedAt={})",
                        room.getRoomId(), room.getStatus(), room.getUpdatedAt());
            }
        }
    }

    private boolean isExpired(Room room, long now) {
        long age = now - room.getUpdatedAt();
        switch (room.getStatus()) {
            case WAITING:
                return age > WAITING_ROOM_EXPIRATION_MS;
            case ACTIVE:
                return age > ACTIVE_ROOM_STALE_MS;
            case COMPLETED:
                return age > COMPLETED_ROOM_EXPIRATION_MS;
            case ABANDONED:
                return age > ABANDONED_ROOM_EXPIRATION_MS;
            default:
                return false;
        }
    }

    public void shutdownCleanupExecutor() {
        cleanupExecutor.shutdownNow();
    }
}
