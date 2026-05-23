package com.example.ChessGame.controller.dto;

public class RoomRequest {
    private String playerName;
    private String playerToken;

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

    public String getPlayerToken() {
        return playerToken;
    }

    public void setPlayerToken(String playerToken) {
        this.playerToken = playerToken;
    }
}
