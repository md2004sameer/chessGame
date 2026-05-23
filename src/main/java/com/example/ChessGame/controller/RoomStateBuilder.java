package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.MoveResponse;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Room;

import java.util.ArrayList;
import java.util.List;

public class RoomStateBuilder {
    public static MoveResponse build(Room room, Game game) {
        MoveResponse resp = new MoveResponse();
        if (game == null) {
            resp.setSuccess(false);
            resp.setMessage("Waiting for opponent or invalid room");
            if (room != null) {
                resp.setRoomStatus(room.getStatus().toString());
                resp.setPlayerCount(room.isFull() ? 2 : 1);
            }
            return resp;
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
            if (room.getLastMoveFrom() != null && room.getLastMoveTo() != null) {
                resp.setPlayerMove(new MoveResponse.MoveDto(
                        room.getLastMoveFrom(), room.getLastMoveTo(), room.getLastMovePromotion()));
                resp.setLastMoveNumber(room.getLastMoveNumber());
            }
        }

        return resp;
    }
}
