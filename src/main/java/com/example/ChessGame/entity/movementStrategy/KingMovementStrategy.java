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

        // King can move one square in any direction
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);
        boolean isStandardKingMove = rowDiff <= 1 && colDiff <= 1 && (rowDiff != 0 || colDiff != 0);

        // Castling is a special king move: two squares horizontally with an unmoved rook on the same rank
        boolean isCastlingMove = rowDiff == 0 && colDiff == 2;
        if (isCastlingMove) {
            int rookCol = endCol > startCol ? 7 : 0;
            Cell rookCell = board.getCell(startRow, rookCol);
            if (rookCell.getPiece() == null || !(rookCell.getPiece() instanceof com.example.ChessGame.entity.piece.Rook)) {
                return false;
            }
            if (rookCell.getPiece().isWhite() != start.getPiece().isWhite()) {
                return false;
            }

            // All squares between king and rook must be empty
            int rangeStart = Math.min(startCol, rookCol) + 1;
            int rangeEnd = Math.max(startCol, rookCol) - 1;
            for (int c = rangeStart; c <= rangeEnd; c++) {
                if (!board.isCellEmpty(startRow, c)) {
                    return false;
                }
            }

            return board.isCellEmpty(endRow, endCol);
        }

        // Check if destination is empty or has enemy piece
        return isStandardKingMove && (board.isCellEmpty(endRow, endCol) ||
               (end.getPiece() != null && start.getPiece().isWhite() != end.getPiece().isWhite()));
    }
}
