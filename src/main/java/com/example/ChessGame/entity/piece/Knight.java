package com.example.ChessGame.entity.piece;

import com.example.ChessGame.entity.Piece;
import com.example.ChessGame.entity.movementStrategy.KnightMovementStrategy;

public class Knight extends Piece {
    public Knight(boolean isWhite) {
        super(isWhite, new KnightMovementStrategy());
    }

    @Override
    public String getDisplayChar() {
        return isWhite() ? "♘" : "♞";
    }
}
