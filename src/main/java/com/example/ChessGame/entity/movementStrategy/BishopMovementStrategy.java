package com.example.ChessGame.entity.movementStrategy;

import com.example.ChessGame.entity.Board;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.MovementStrategy;

public class BishopMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell start, Cell end) {
        // Get the coordinates
        int startRow = start.getRow();
        int startCol = start.getCol();
        int endRow = end.getRow();
        int endCol = end.getCol();

        // Check if the move is diagonal (absolute difference between rows and columns should be equal)
        if (Math.abs(endRow - startRow) != Math.abs(endCol - startCol)) {
            return false;
        }

        // Determine the direction of movement
        int rowDirection = Integer.compare(endRow - startRow, 0);
        int colDirection = Integer.compare(endCol - startCol, 0);

        // Check path for any obstacles
        int currentRow = startRow + rowDirection;
        int currentCol = startCol + colDirection;

        while (currentRow != endRow && currentCol != endCol) {
            if (!board.isCellEmpty(currentRow, currentCol)) {
                return false; // Path is blocked
            }
            currentRow += rowDirection;
            currentCol += colDirection;
        }

        // Check if destination is empty or contains an enemy piece
        return board.isCellEmpty(endRow, endCol) ||
               (end.getPiece() != null && start.getPiece().isWhite() != end.getPiece().isWhite());
    }
}
