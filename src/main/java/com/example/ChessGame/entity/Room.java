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
    private String creatorToken;
    private String joinerName;
    private String joinerToken;
    private Game game;
    private RoomStatus status;
    private long createdAt;
    private long updatedAt;
    private String lastMoveFrom;
    private String lastMoveTo;
    private String lastMovePromotion;
    private long lastMoveNumber;
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

    public String getCreatorToken() {
        return creatorToken;
    }

    public void setCreatorToken(String creatorToken) {
        this.creatorToken = creatorToken;
    }

    public String getJoinerToken() {
        return joinerToken;
    }

    public void setJoinerToken(String joinerToken) {
        this.joinerToken = joinerToken;
    }

    public boolean isCreatorAuthorized(String playerName, String playerToken) {
        return creatorName.equals(playerName) && creatorToken != null && creatorToken.equals(playerToken);
    }

    public boolean isJoinerAuthorized(String playerName, String playerToken) {
        return joinerName != null && joinerName.equals(playerName) && joinerToken != null && joinerToken.equals(playerToken);
    }

    public boolean isPlayerAuthorized(String playerName, String playerToken) {
        return isCreatorAuthorized(playerName, playerToken) || isJoinerAuthorized(playerName, playerToken);
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

    public String getLastMoveFrom() {
        return lastMoveFrom;
    }

    public String getLastMoveTo() {
        return lastMoveTo;
    }

    public String getLastMovePromotion() {
        return lastMovePromotion;
    }

    public long getLastMoveNumber() {
        return lastMoveNumber;
    }

    public void setLastMove(String from, String to, String promotion) {
        this.lastMoveFrom = from;
        this.lastMoveTo = to;
        this.lastMovePromotion = promotion;
        this.lastMoveNumber++;
        this.updatedAt = System.currentTimeMillis();
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
