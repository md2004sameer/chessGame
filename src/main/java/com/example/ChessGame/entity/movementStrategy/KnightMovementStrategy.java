package com.example.ChessGame.entity.movementStrategy;

import com.example.ChessGame.entity.Board;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.MovementStrategy;

public class KnightMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell start, Cell end) {
        int startRow = start.getRow();
        int startCol = start.getCol();
        int endRow = end.getRow();
        int endCol = end.getCol();

        // Knights move in L-shape: 2 squares in one direction and 1 square perpendicular
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        // Check if the move forms an L-shape (2,1) or (1,2)
        boolean isValidMove = (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);

        // Knights can jump over pieces, so we only need to check the destination
        return isValidMove && (board.isCellEmpty(endRow, endCol) ||
               (end.getPiece() != null && start.getPiece().isWhite() != end.getPiece().isWhite()));
    }
}
