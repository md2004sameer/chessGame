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

@Service
public class RoomService {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    /**
     * Create a new game room
     */
    public String createRoom(String creatorName) {
        String name = validatePlayerName(creatorName);
        
        String id = generateRoomId();
        Room room = new Room(id, name);
        
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
    public synchronized String joinRoom(String roomId, String playerName) throws IllegalArgumentException {
        logger.info("Join room requested: roomId={}, playerName={}", roomId, playerName);
        
        String name = validatePlayerName(playerName);
        Room room = rooms.get(roomId);
        
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        // Existing players can refresh/reconnect even after the game is active.
        if (name.equals(room.getCreatorName())) {
            logger.info("Creator {} rejoined room {}", name, roomId);
            return "white";
        }

        if (name.equals(room.getJoinerName())) {
            logger.info("Player {} rejoined room {} as black", name, roomId);
            return "black";
        }

        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new IllegalArgumentException("Room is not available (status: " + room.getStatus() + ")");
        }

        // Only allow one joiner
        if (room.isFull()) {
            throw new IllegalArgumentException("Room is full");
        }

        // Set joiner and update game
        room.setJoinerName(name);
        room.setStatus(Room.RoomStatus.ACTIVE);
        
        if (room.getGame() != null) {
            room.getGame().setBlackPlayer(new Player(name, false));
        }
        
        logger.info("Player {} joined room {} as black", name, roomId);
        return "black";
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
        Room room = rooms.get(roomId);
        if (room == null || room.getGame() == null) {
            return false;
        }

        if (room.getStatus() != Room.RoomStatus.ACTIVE || !room.isFull()) {
            logger.warn("Move rejected before room is active: roomId={}, playerName={}, status={}",
                    roomId, playerName, room.getStatus());
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

        game.makeMove(move, game.getCurrentTurn());
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
}
