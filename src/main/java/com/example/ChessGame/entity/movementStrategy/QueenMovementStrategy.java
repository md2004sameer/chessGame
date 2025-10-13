package com.example.ChessGame.entity.movementStrategy;

import com.example.ChessGame.entity.Board;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.MovementStrategy;

public class QueenMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell start, Cell end) {
        int startRow = start.getRow();
        int startCol = start.getCol();
        int endRow = end.getRow();
        int endCol = end.getCol();

        // Queen can move either like a rook (horizontally/vertically) or like a bishop (diagonally)
        boolean isDiagonal = Math.abs(endRow - startRow) == Math.abs(endCol - startCol);
        boolean isStraight = startRow == endRow || startCol == endCol;

        if (!isDiagonal && !isStraight) {
            return false;
        }

        // Determine the direction of movement
        int rowDirection = Integer.compare(endRow - startRow, 0);
        int colDirection = Integer.compare(endCol - startCol, 0);

        // Check path for obstacles
        int currentRow = startRow + rowDirection;
        int currentCol = startCol + colDirection;

        while (currentRow != endRow || currentCol != endCol) {
            if (!board.isCellEmpty(currentRow, currentCol)) {
                return false;
            }
            currentRow += rowDirection;
            currentCol += colDirection;
        }

        // Check if destination is empty or has enemy piece
        return board.isCellEmpty(endRow, endCol) ||
               (end.getPiece() != null && start.getPiece().isWhite() != end.getPiece().isWhite());
    }
}
