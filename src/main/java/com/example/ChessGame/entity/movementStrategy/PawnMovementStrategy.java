package com.example.ChessGame.entity.movementStrategy;

import com.example.ChessGame.entity.Board;
import com.example.ChessGame.entity.Cell;
import com.example.ChessGame.entity.MovementStrategy;

public class PawnMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell start, Cell end) {
        int startRow = start.getRow();
        int startCol = start.getCol();
        int endRow = end.getRow();
        int endCol = end.getCol();

        // Determine direction based on color (white moves up, black moves down)
        int direction = start.getPiece().isWhite() ? -1 : 1;

        // Normal one square forward move
        if (startCol == endCol && endRow == startRow + direction) {
            return board.isCellEmpty(endRow, endCol);
        }

        // First move - can move two squares forward
        boolean isFirstMove = (start.getPiece().isWhite() && startRow == 6) ||
                            (!start.getPiece().isWhite() && startRow == 1);
        if (isFirstMove && startCol == endCol && endRow == startRow + (2 * direction)) {
            return board.isCellEmpty(startRow + direction, startCol) &&
                   board.isCellEmpty(endRow, endCol);
        }

        // Diagonal capture or en-passant capture
        boolean isDiagonalMove = Math.abs(endCol - startCol) == 1 && endRow == startRow + direction;
        if (isDiagonalMove) {
            if (end.getPiece() != null) {
                return start.getPiece().isWhite() != end.getPiece().isWhite();
            }

            String enPassantTarget = board.getEnPassantTarget();
            if (enPassantTarget != null && !enPassantTarget.equals("-")) {
                String endPos = String.format("%c%d", (char)('a' + endCol), 8 - endRow);
                return endPos.equals(enPassantTarget);
            }
        }

        return false;
    }
}
