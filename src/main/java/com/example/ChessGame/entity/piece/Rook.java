package com.example.ChessGame.entity.piece;

import com.example.ChessGame.entity.Piece;
import com.example.ChessGame.entity.movementStrategy.RookMovementStrategy;

public class Rook extends Piece {
    public Rook(boolean isWhite) {
        super(isWhite, new RookMovementStrategy());
    }

    @Override
    public String getDisplayChar() {
        return isWhite() ? "♖" : "♜";
    }
}
