package com.example.ChessGame.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum RoomStatus {
        WAITING,      // Waiting for second player
        ACTIVE,       // Both players joined, game active
        COMPLETED,    // Game finished
        ABANDONED     // Room abandoned
    }

    private String roomId;
    private String creatorName;
    private String joinerName;
    private Game game;
    private RoomStatus status;
    private long createdAt;
    private long updatedAt;
    private Map<String, Object> metadata; // For future extensions

    public Room(String roomId, String creatorName) {
        this.roomId = roomId;
        this.creatorName = creatorName;
        this.status = RoomStatus.WAITING;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.metadata = new HashMap<>();
    }

    public String getRoomId() {
        return roomId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public String getJoinerName() {
        return joinerName;
    }

    public void setJoinerName(String joinerName) {
        this.joinerName = joinerName;
        this.updatedAt = System.currentTimeMillis();
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
        this.updatedAt = System.currentTimeMillis();
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isFull() {
        return joinerName != null && !joinerName.isEmpty();
    }

    public boolean hasPlayer(String playerName) {
        return creatorName.equals(playerName) || (joinerName != null && joinerName.equals(playerName));
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId='" + roomId + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", joinerName='" + joinerName + '\'' +
                ", status=" + status +
                ", playerCount=" + (isFull() ? 2 : 1) +
                '}';
    }
}
