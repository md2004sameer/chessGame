package com.example.ChessGame.entity;

public class Move {
    private Cell start;
    private Cell end;

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
}
