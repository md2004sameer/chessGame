package com.example.ChessGame.entity.piece;

import com.example.ChessGame.entity.Piece;
import com.example.ChessGame.entity.movementStrategy.KingMovementStrategy;

public class King extends Piece {

    public King(boolean isWhite) {
        super(isWhite, new KingMovementStrategy());
    }

    @Override
    public String getDisplayChar() {
        return isWhite() ? "♔" : "♚";
    }
}
