package com.example.ChessGame.entity;

public class Move {
    private Cell start;
    private Cell end;
    private String promotion; // optional: "queen", "rook", "bishop", "knight"

    public Cell getEnd() {
        return end;
    }
    public Cell getStart() {
        return start;
    }
    public Move(Cell start, Cell end) {
        this.start = start;
        this.end = end;
    }

    public Move(Cell start, Cell end, String promotion) {
        this.start = start;
        this.end = end;
        this.promotion = promotion;
    }

    public String getPromotion() {
        return promotion;
    }
}
