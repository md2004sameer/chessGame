package com.example.ChessGame.controller.dto;

import java.util.List;

public class MoveResponse {
    private boolean success;
    private String message;
    private List<List<CellDto>> board;
    private String currentPlayer;
    private String gameStatus;

    public static class CellDto {
        public String position;
        public String display;
        public String type;
        public Object color;
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
}
