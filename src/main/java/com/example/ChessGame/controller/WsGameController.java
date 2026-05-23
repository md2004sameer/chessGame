package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.MoveResponse;
import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Room;
import com.example.ChessGame.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;



@Controller
public class WsGameController {
    private final RoomService roomService;
    private final com.example.ChessGame.service.MessagingService messagingService;

    public WsGameController(RoomService roomService, com.example.ChessGame.service.MessagingService messagingService) {
        this.roomService = roomService;
        this.messagingService = messagingService;
    }

    public static class JoinMessage {
        public String roomId;
        public String playerName;
        public String playerToken;
    }

    public static class MoveMessage {
        public String roomId;
        public String from;
        public String to;
        public String promotion;
        public String playerName;
        public String playerToken;
    }

    @MessageMapping("/rooms/join")
    public void join(JoinMessage msg, org.springframework.messaging.simp.SimpMessageHeaderAccessor sha) {
        org.slf4j.LoggerFactory.getLogger(WsGameController.class).info("WS join requested: roomId={}, playerName={}", msg.roomId, msg.playerName);
        com.example.ChessGame.service.RoomService.JoinResult joinResult;
        try {
            // If the creator connects via WS without providing the token (common when they created via REST),
            // send the creator token privately and broadcast the state without enforcing token check here.
            Room existing = roomService.getRoom(msg.roomId);
            if (existing != null && existing.getCreatorName().equals(msg.playerName) && (msg.playerToken == null || msg.playerToken.isEmpty())) {
                java.util.Map<String, Object> tokenPayload = new java.util.HashMap<>();
                tokenPayload.put("playerToken", existing.getCreatorToken());
                tokenPayload.put("role", "white");
                try {
                    messagingService.sendToUser(sha.getSessionId(), "/queue/rooms/" + msg.roomId + "/token", tokenPayload);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(WsGameController.class).warn("Failed to send private token to session {}: {}", sha.getSessionId(), e.getMessage());
                }
                org.slf4j.LoggerFactory.getLogger(WsGameController.class).info("WS creator reconnect handled: roomId={}, playerName={}", msg.roomId, msg.playerName);
                sendState(msg.roomId);
                return;
            }

            joinResult = roomService.joinRoom(msg.roomId, msg.playerName, msg.playerToken);
            org.slf4j.LoggerFactory.getLogger(WsGameController.class).info("WS join success/rejoin: roomId={}, playerName={}", msg.roomId, msg.playerName);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WsGameController.class).warn("WS join failed: roomId={}, playerName={}, error={}", msg.roomId, msg.playerName, e.getMessage());
            MoveResponse err = new MoveResponse();
            err.setSuccess(false);
            err.setMessage("Could not join room: " + e.getMessage());
            messagingService.sendToTopic("/topic/rooms/" + msg.roomId, err);
            return;
        }

        // send the playerToken privately to the joining session only
        try {
            java.util.Map<String, Object> tokenPayload = new java.util.HashMap<>();
            tokenPayload.put("playerToken", joinResult.getPlayerToken());
            tokenPayload.put("role", joinResult.getRole());
            messagingService.sendToUser(sha.getSessionId(), "/queue/rooms/" + msg.roomId + "/token", tokenPayload);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WsGameController.class).warn("Failed to send private token to session {}: {}", sha.getSessionId(), e.getMessage());
        }

        // after joining, broadcast full state to the public topic
        sendState(msg.roomId);
    }

    @MessageMapping("/rooms/{roomId}/move")
    public void move(@DestinationVariable String roomId, MoveMessage msg) {
        boolean ok = roomService.applyMove(roomId, msg.from, msg.to, msg.promotion, msg.playerName, msg.playerToken);
        if (!ok) {
            MoveResponse resp = new MoveResponse();
            resp.setSuccess(false);
            resp.setMessage("Invalid move or not your turn");
            messagingService.sendToTopic("/topic/rooms/" + roomId, resp);
            // still broadcast the unchanged state so clients remain consistent
            sendState(roomId);
            return;
        }
        // broadcast updated state
        sendState(roomId);
    }

    private void sendState(String roomId) {
        Room room = roomService.getRoom(roomId);
        Game game = roomService.getGame(roomId);
        MoveResponse resp = RoomStateBuilder.build(room, game);
        messagingService.sendToTopic("/topic/rooms/" + roomId, resp);
    }
}
