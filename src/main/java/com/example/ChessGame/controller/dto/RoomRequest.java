package com.example.ChessGame.controller.dto;

public class RoomRequest {
    private String playerName;

    public RoomRequest() {
    }

    public RoomRequest(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
