package com.example.ChessGame.entity;
;

public class GameStateChecker {
    private final Board board;

    public GameStateChecker(Board board) {
        this.board = board;
    }

    public boolean isKingInCheck(boolean isWhiteKing) {
        // Find the king's position
        Cell kingCell = findKing(isWhiteKing);
        if (kingCell == null) return false;

        // Check if any opponent's piece can capture the king
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Cell currentCell = board.getCell(i, j);
                Piece piece = currentCell.getPiece();
                if (piece != null && piece.isWhite() != isWhiteKing) {
                    if (piece.canMove(board, currentCell, kingCell)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCheckmate(boolean isWhiteKing) {
        if (!isKingInCheck(isWhiteKing)) {
            return false;
        }

        // Check if any move can get the king out of check
        return !hasValidMoves(isWhiteKing);
    }

    public boolean isStalemate(boolean isWhiteKing) {
        if (isKingInCheck(isWhiteKing)) {
            return false;
        }

        // Stalemate occurs when the player is not in check but has no valid moves
        return !hasValidMoves(isWhiteKing);
    }

    private boolean hasValidMoves(boolean isWhite) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Cell start = board.getCell(i, j);
                Piece piece = start.getPiece();

                if (piece != null && piece.isWhite() == isWhite) {
                    if (hasValidMovesForPiece(start)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasValidMovesForPiece(Cell start) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Cell end = board.getCell(i, j);
                if (start.getPiece().canMove(board, start, end)) {
                    // Try the move and see if it prevents check
                    if (isMoveSafe(start, end)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isMoveSafe(Cell start, Cell end) {
        // Store the current state
        Piece startPiece = start.getPiece();
        Piece endPiece = end.getPiece();
        boolean isWhite = startPiece.isWhite();

        // Make the move
        end.setPiece(startPiece);
        start.setPiece(null);

        // Check if the king is still in check
        boolean isSafe = !isKingInCheck(isWhite);

        // Restore the position
        start.setPiece(startPiece);
        end.setPiece(endPiece);

        return isSafe;
    }

    private Cell findKing(boolean isWhiteKing) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Cell cell = board.getCell(i, j);
                Piece piece = cell.getPiece();
                if (piece instanceof com.example.ChessGame.entity.piece.King && piece.isWhite() == isWhiteKing) {
                    return cell;
                }
            }
        }
        return null;
    }
}
