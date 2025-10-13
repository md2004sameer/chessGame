package com.example.ChessGame.entity;

public interface MovementStrategy {
    public boolean canMove(Board board, Cell start, Cell end);
}
