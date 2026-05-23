package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.MoveResponse;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Room;
import com.example.ChessGame.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

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
    }

    public static class MoveMessage {
        public String roomId;
        public String from;
        public String to;
        public String promotion;
        public String playerName;
    }

    @MessageMapping("/rooms/join")
    public void join(JoinMessage msg) {
        org.slf4j.LoggerFactory.getLogger(WsGameController.class).info("WS join requested: roomId={}, playerName={}", msg.roomId, msg.playerName);
        try {
            String role = roomService.joinRoom(msg.roomId, msg.playerName);
            org.slf4j.LoggerFactory.getLogger(WsGameController.class).info("WS join success/rejoin: roomId={}, playerName={}, role={}", msg.roomId, msg.playerName, role);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WsGameController.class).warn("WS join failed: roomId={}, playerName={}, error={}", msg.roomId, msg.playerName, e.getMessage());
            MoveResponse err = new MoveResponse();
            err.setSuccess(false);
            err.setMessage("Could not join room: " + e.getMessage());
            messagingService.sendToTopic("/topic/rooms/" + msg.roomId, err);
            return;
        }
        // after joining, broadcast full state
        sendState(msg.roomId);
    }

    @MessageMapping("/rooms/{roomId}/move")
    public void move(@DestinationVariable String roomId, MoveMessage msg) {
        boolean ok = roomService.applyMove(roomId, msg.from, msg.to, msg.promotion, msg.playerName);
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
        MoveResponse resp = new MoveResponse();
        if (game == null) {
            resp.setSuccess(false);
            resp.setMessage("Waiting for opponent or invalid room");
            if (room != null) {
                resp.setRoomStatus(room.getStatus().toString());
                resp.setPlayerCount(room.isFull() ? 2 : 1);
            }
            messagingService.sendToTopic("/topic/rooms/" + roomId, resp);
            return;
        }

        List<List<MoveResponse.CellDto>> boardState = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            List<MoveResponse.CellDto> rowList = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                Cell cell = game.getBoard().getCell(r, c);
                MoveResponse.CellDto cellDto = new MoveResponse.CellDto();
                char file = (char) ('a' + c);
                int rank = 8 - r;
                String pos = String.format("%c%d", file, rank);
                cellDto.position = pos;
                if (cell.getPiece() != null) {
                    cellDto.display = cell.getPiece().getDisplayChar();
                    cellDto.type = cell.getPiece().getType();
                    cellDto.color = cell.getPiece().isWhite();
                } else {
                    cellDto.display = "";
                    cellDto.type = "";
                    cellDto.color = "";
                }
                rowList.add(cellDto);
            }
            boardState.add(rowList);
        }
        resp.setSuccess(true);
        resp.setBoard(boardState);
        resp.setCurrentPlayer(game.getCurrentTurn().getName());
        resp.setGameStatus(game.getGameStatus().toString());
        // include player names so clients can update UI when someone joins
        resp.setWhitePlayerName(game.getWhitePlayer().getName());
        resp.setBlackPlayerName(game.getBlackPlayer().getName());
        if (room != null) {
            resp.setRoomStatus(room.getStatus().toString());
            resp.setPlayerCount(room.isFull() ? 2 : 1);
        }
        messagingService.sendToTopic("/topic/rooms/" + roomId, resp);
    }
}
