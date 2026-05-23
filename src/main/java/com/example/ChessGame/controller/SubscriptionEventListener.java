package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.MoveResponse;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Room;
import com.example.ChessGame.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Component
public class SubscriptionEventListener implements ApplicationListener<SessionSubscribeEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionEventListener.class);
    private final RoomService roomService;
    private final com.example.ChessGame.service.MessagingService messagingService;

    public SubscriptionEventListener(RoomService roomService, com.example.ChessGame.service.MessagingService messagingService) {
        this.roomService = roomService;
        this.messagingService = messagingService;
    }

    @Override
    public void onApplicationEvent(@NonNull SessionSubscribeEvent event) {
        String dest = SimpMessageHeaderAccessor.getDestination(event.getMessage().getHeaders());
        if (dest == null) return;
        if (!dest.startsWith("/topic/rooms/")) return;
        String roomId = dest.substring("/topic/rooms/".length());
        logger.info("Subscription to {} detected, broadcasting current state", dest);
        // Build and broadcast state (safe to broadcast to topic; subscribers including the new one will receive it)
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
            messagingService.sendToTopic(dest, resp);
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
        resp.setWhitePlayerName(game.getWhitePlayer().getName());
        resp.setBlackPlayerName(game.getBlackPlayer().getName());
        if (room != null) {
            resp.setRoomStatus(room.getStatus().toString());
            resp.setPlayerCount(room.isFull() ? 2 : 1);
        }
        messagingService.sendToTopic(dest, resp);
    }
}
