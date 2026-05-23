package com.example.ChessGame.controller.dto;

public class RoomResponse {
    private String roomId;
    private String creatorName;
    private String joinedPlayerName;
    private String playerToken;
    private String status; // "waiting", "active", "completed"
    private long createdAt;
    private String joinUrl;
    private int playerCount;

    public RoomResponse() {
    }

    public RoomResponse(String roomId, String creatorName, String status) {
        this.roomId = roomId;
        this.creatorName = creatorName;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.playerCount = 1;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getJoinedPlayerName() {
        return joinedPlayerName;
    }

    public void setJoinedPlayerName(String joinedPlayerName) {
        this.joinedPlayerName = joinedPlayerName;
    }

    public String getPlayerToken() {
        return playerToken;
    }

    public void setPlayerToken(String playerToken) {
        this.playerToken = playerToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }
}
