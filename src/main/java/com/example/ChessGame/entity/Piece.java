package com.example.ChessGame.entity;

public abstract class Piece {
    MovementStrategy movementStrategy;
    boolean isWhite;
    boolean isKilled;
    public Piece(boolean isWhite, MovementStrategy movementStrategy) {
        this.isWhite = isWhite;
        this.isKilled = false;
        this.movementStrategy = movementStrategy;
    }
    public boolean isWhite() {
        return isWhite;
    }
    public boolean isKilled() {
        return isKilled;
    }
    public void setKilled(boolean killed) {
        isKilled = killed;
    }
    public boolean canMove(Board board, Cell start, Cell end) {
        return movementStrategy.canMove(board, start, end);
    }
    public abstract String getDisplayChar();
}
