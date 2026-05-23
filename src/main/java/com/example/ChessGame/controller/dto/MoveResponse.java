package com.example.ChessGame.controller.dto;

import java.util.List;

public class MoveResponse {
    private boolean success;
    private String message;
    private List<List<CellDto>> board;
    private String currentPlayer;
    private String gameStatus;
    private String whitePlayerName;
    private String blackPlayerName;
    private String roomStatus;
    private int playerCount;
    private MoveDto playerMove;
    private MoveDto aiMove;
    private long lastMoveNumber;

    public static class CellDto {
        public String position;
        public String display;
        public String type;
        public Object color;
    }

    public static class MoveDto {
        public String from;
        public String to;
        public String promotion;

        public MoveDto() {}

        public MoveDto(String from, String to, String promotion) {
            this.from = from;
            this.to = to;
            this.promotion = promotion;
        }
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<List<CellDto>> getBoard() { return board; }
    public void setBoard(List<List<CellDto>> board) { this.board = board; }
    public String getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(String currentPlayer) { this.currentPlayer = currentPlayer; }
    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }
    public String getWhitePlayerName() { return whitePlayerName; }
    public void setWhitePlayerName(String whitePlayerName) { this.whitePlayerName = whitePlayerName; }
    public String getBlackPlayerName() { return blackPlayerName; }
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }
    public String getRoomStatus() { return roomStatus; }
    public void setRoomStatus(String roomStatus) { this.roomStatus = roomStatus; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public MoveDto getPlayerMove() { return playerMove; }
    public void setPlayerMove(MoveDto playerMove) { this.playerMove = playerMove; }
    public MoveDto getAiMove() { return aiMove; }
    public void setAiMove(MoveDto aiMove) { this.aiMove = aiMove; }
    public long getLastMoveNumber() { return lastMoveNumber; }
    public void setLastMoveNumber(long lastMoveNumber) { this.lastMoveNumber = lastMoveNumber; }
}
