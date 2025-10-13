package com.example.ChessGame.entity;


import com.example.ChessGame.entity.piece.*;

public class PieceFactory {
    public static Piece createPiece(String type, boolean isWhite) {
        switch (type.toLowerCase()) {
            case "pawn":
                return new Pawn(isWhite);
            case "rook":
                return new Rook(isWhite);
            case "knight":
                return new Knight(isWhite);
            case "bishop":
                return new Bishop(isWhite);
            case "queen":
                return new Queen(isWhite);
            case "king":
                return new King(isWhite);
            default:
                throw new IllegalArgumentException("Unknown piece type: " + type);
        }
    }
}
