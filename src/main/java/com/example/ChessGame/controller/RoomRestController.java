package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.*;
import com.example.ChessGame.entity.Room;
import com.example.ChessGame.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for room management.
 * Provides endpoints for creating, joining, and managing game rooms.
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

    // ─────────────────────────────────────────────────────────────
    // POST /api/rooms  — create a new room
    // ─────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody RoomRequest request) {
        if (request == null || isBlank(request.getPlayerName())) {
            return badRequest("INVALID_REQUEST", "Player name is required");
        }

        try {
            String playerName = request.getPlayerName().trim();
            logger.info("Creating room for player: {}", playerName);

            String roomId = roomService.createRoom(playerName);

            RoomResponse response = new RoomResponse(roomId, playerName, "waiting");
            response.setJoinUrl("/api/rooms/" + roomId + "/join");
            response.setPlayerCount(1);

            logger.info("Room created: {}", roomId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Create room rejected: {}", e.getMessage());
            return badRequest("INVALID_REQUEST", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error creating room", e);
            return serverError("Failed to create room");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/rooms/{roomId}/join  — join an existing room
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId,
                                      @RequestBody RoomRequest request) {
        if (request == null || isBlank(request.getPlayerName())) {
            return badRequest("INVALID_REQUEST", "Player name is required");
        }

        try {
            String playerName = request.getPlayerName().trim();
            logger.info("Player '{}' attempting to join room {}", playerName, roomId);

            String role = roomService.joinRoom(roomId, playerName);

            Room room = roomService.getRoom(roomId);
            RoomResponse response = new RoomResponse(
                    roomId,
                    room.getCreatorName(),
                    room.getStatus().toString().toLowerCase()
            );
            response.setJoinedPlayerName(room.getJoinerName());
            response.setPlayerCount(room.isFull() ? 2 : 1);

            logger.info("Player '{}' joined room {} as {}", playerName, roomId, role);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Join room {} rejected: {}", roomId, e.getMessage());
            return badRequest("INVALID_JOIN", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error joining room {}", roomId, e);
            return serverError("Failed to join room");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/rooms/{roomId}  — get a single room
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        try {
            Room room = roomService.getRoom(roomId);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("NOT_FOUND", "Room not found", 404));
            }

            RoomResponse response = new RoomResponse(
                    roomId,
                    room.getCreatorName(),
                    room.getStatus().toString().toLowerCase()
            );
            response.setJoinedPlayerName(room.getJoinerName());
            response.setPlayerCount(room.isFull() ? 2 : 1);
            response.setCreatedAt(room.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving room {}", roomId, e);
            return serverError("Failed to retrieve room");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/rooms  — list all rooms
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listActiveRooms() {
        try {
            List<RoomResponse> responses = roomService.getActiveRooms().stream()
                    .map(room -> {
                        RoomResponse r = new RoomResponse(
                                room.getRoomId(),
                                room.getCreatorName(),
                                room.getStatus().toString().toLowerCase()
                        );
                        r.setJoinedPlayerName(room.getJoinerName());
                        r.setPlayerCount(room.isFull() ? 2 : 1);
                        r.setCreatedAt(room.getCreatedAt());
                        return r;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Error listing rooms", e);
            return serverError("Failed to list rooms");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/rooms/health  — health check
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> body = new HashMap<>();
        body.put("status", "healthy");
        body.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(body);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/rooms/{roomId}/players/{playerName}  — check membership
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/{roomId}/players/{playerName}")
    public ResponseEntity<?> checkPlayerInRoom(@PathVariable String roomId,
                                               @PathVariable String playerName) {
        try {
            boolean isIn = roomService.isPlayerInRoom(roomId, playerName);

            Map<String, Object> body = new HashMap<>();
            body.put("roomId", roomId);
            body.put("playerName", playerName);
            body.put("isInRoom", isIn);

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            logger.error("Error checking player {} in room {}", playerName, roomId, e);
            return serverError("Failed to check player");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ResponseEntity<ErrorResponse> badRequest(String error, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(error, message, 400));
    }

    private ResponseEntity<ErrorResponse> serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", message, 500));
    }
}