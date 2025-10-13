package com.example.ChessGame.entity.piece;

import com.example.ChessGame.entity.Piece;
import com.example.ChessGame.entity.movementStrategy.QueenMovementStrategy;

public class Queen extends Piece {
    public Queen(boolean isWhite) {
        super(isWhite, new QueenMovementStrategy());
    }

    @Override
    public String getDisplayChar() {
        return isWhite() ? "♕" : "♛";
    }
}
