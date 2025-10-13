package com.example.ChessGame.entity.piece;


import com.example.ChessGame.entity.Piece;
import com.example.ChessGame.entity.movementStrategy.BishopMovementStrategy;

public class Bishop extends Piece {
    public Bishop(boolean isWhite) {
        super(isWhite, new BishopMovementStrategy());
    }

    @Override
    public String getDisplayChar() {
        return isWhite() ? "♗" : "♝";
    }
}
