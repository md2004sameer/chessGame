package com.example.ChessGame.entity;

public class Cell {
    int row;
    int col;
    Piece piece;
    boolean isBlack;

    public Cell(int row, int col, Piece piece, boolean isBlack) {
        this.row = row;
        this.col = col;
        this.piece = piece;
        this.isBlack = isBlack;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public boolean isBlack() {
        return isBlack;
    }
}
