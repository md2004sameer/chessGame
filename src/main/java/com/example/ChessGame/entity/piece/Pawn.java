package com.example.ChessGame.entity.piece;

import com.example.ChessGame.entity.Piece;
import com.example.ChessGame.entity.movementStrategy.PawnMovementStrategy;

public class Pawn extends Piece {
    public Pawn(boolean isWhite) {
        super(isWhite, new PawnMovementStrategy());
    }

    @Override
    public String getDisplayChar() {
        return isWhite() ? "♙" : "♟";
    }
}
