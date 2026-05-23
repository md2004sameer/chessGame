package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.*;
import com.example.ChessGame.entity.Room;
import com.example.ChessGame.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for room management
 * Provides endpoints for creating, joining, and managing game rooms
 */
@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomRestController {
    private static final Logger logger = LoggerFactory.getLogger(RoomRestController.class);
    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Create a new room
     * POST /api/rooms
     */
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody RoomRequest request) {
        try {
            // Validate request
            if (request == null || request.getPlayerName() == null || request.getPlayerName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_REQUEST", "Player name is required", 400));
            }
            
            logger.info("Creating room for player: {}", request.getPlayerName());
            String roomId = roomService.createRoom(request.getPlayerName());
            
            RoomResponse response = new RoomResponse(roomId, request.getPlayerName(), "waiting");
            response.setJoinUrl(String.format("/api/rooms/%s/join", roomId));
            response.setPlayerCount(1);
            
            logger.info("Room created successfully: {}", roomId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create room: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_REQUEST", e.getMessage(), 400));
        } catch (Exception e) {
            logger.error("Error creating room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to create room", 500));
        }
    }

    /**
     * Join an existing room
     * POST /api/rooms/{roomId}/join
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId, @RequestBody RoomRequest request) {
        try {
            // Validate request
            if (request == null || request.getPlayerName() == null || request.getPlayerName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_REQUEST", "Player name is required", 400));
            }
            
            logger.info("Player {} attempting to join room {}", request.getPlayerName(), roomId);
            String role = roomService.joinRoom(roomId, request.getPlayerName());
            
            Room room = roomService.getRoom(roomId);
            RoomResponse response = new RoomResponse(roomId, room.getCreatorName(), room.getStatus().toString().toLowerCase());
            response.setJoinedPlayerName(room.getJoinerName());
            response.setPlayerCount(room.isFull() ? 2 : 1);
            
            logger.info("Player {} joined room {} as {}", request.getPlayerName(), roomId, role);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to join room {}: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_JOIN", e.getMessage(), 400));
        } catch (Exception e) {
            logger.error("Error joining room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to join room", 500));
        }
    }

    /**
     * Get room information
     * GET /api/rooms/{roomId}
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        try {
            Room room = roomService.getRoom(roomId);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("NOT_FOUND", "Room not found", 404));
            }
            
            RoomResponse response = new RoomResponse(roomId, room.getCreatorName(), room.getStatus().toString().toLowerCase());
            response.setJoinedPlayerName(room.getJoinerName());
            response.setPlayerCount(room.isFull() ? 2 : 1);
            response.setCreatedAt(room.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to retrieve room", 500));
        }
    }

    /**
     * Get all active rooms
     * GET /api/rooms
     */
    @GetMapping
    public ResponseEntity<?> listActiveRooms() {
        try {
            List<Room> activeRooms = roomService.getActiveRooms();
            List<RoomResponse> responses = activeRooms.stream()
                    .map(room -> {
                        RoomResponse r = new RoomResponse(room.getRoomId(), room.getCreatorName(), 
                                                          room.getStatus().toString().toLowerCase());
                        r.setJoinedPlayerName(room.getJoinerName());
                        r.setPlayerCount(room.isFull() ? 2 : 1);
                        r.setCreatedAt(room.getCreatedAt());
                        return r;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error listing rooms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to list rooms", 500));
        }
    }

    /**
     * Health check endpoint
     * GET /api/rooms/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
            put("status", "healthy");
            put("timestamp", String.valueOf(System.currentTimeMillis()));
        }});
    }

    /**
     * Check if player is in a room
     * GET /api/rooms/{roomId}/players/{playerName}
     */
    @GetMapping("/{roomId}/players/{playerName}")
    public ResponseEntity<?> checkPlayerInRoom(@PathVariable String roomId, @PathVariable String playerName) {
        try {
            boolean isIn = roomService.isPlayerInRoom(roomId, playerName);
            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
                put("roomId", roomId);
                put("playerName", playerName);
                put("isInRoom", isIn);
            }});
        } catch (Exception e) {
            logger.error("Error checking player in room", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to check player", 500));
        }
    }
}
