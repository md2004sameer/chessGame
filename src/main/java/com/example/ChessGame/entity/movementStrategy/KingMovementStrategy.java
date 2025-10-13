package com.example.ChessGame.entity.movementStrategy;

import com.example.ChessGame.entity.Board;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.MovementStrategy;

public class KingMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell start, Cell end) {
        int startRow = start.getRow();
        int startCol = start.getCol();
        int endRow = end.getRow();
        int endCol = end.getCol();

        // King can move only one square in any direction
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        // Valid move is when king moves one square horizontally, vertically, or diagonally
        boolean isValidMove = rowDiff <= 1 && colDiff <= 1 && (rowDiff != 0 || colDiff != 0);

        // Check if destination is empty or has enemy piece
        return isValidMove && (board.isCellEmpty(endRow, endCol) ||
               (end.getPiece() != null && start.getPiece().isWhite() != end.getPiece().isWhite()));
    }
}
