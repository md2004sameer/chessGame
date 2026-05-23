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
        Piece startPiece = start.getPiece();
        Piece endPiece = end.getPiece();
        boolean isWhite = startPiece.isWhite();

        boolean isEnPassant = false;
        Cell enPassantCapturedCell = null;
        Piece enPassantCapturedPawn = null;

        boolean isCastling = false;
        Cell castlingRookStart = null;
        Cell castlingRookEnd = null;
        Piece castlingRookPiece = null;

        // Detect en-passant capture
        if (startPiece instanceof com.example.ChessGame.entity.piece.Pawn) {
            int startRow = start.getRow();
            int startCol = start.getCol();
            int endRow = end.getRow();
            int endCol = end.getCol();
            boolean diagonal = Math.abs(endCol - startCol) == 1 && endRow == startRow + (startPiece.isWhite() ? -1 : 1);
            if (diagonal && endPiece == null) {
                String target = board.getEnPassantTarget();
                String endPos = String.format("%c%d", (char)('a' + endCol), 8 - endRow);
                if (target != null && target.equals(endPos)) {
                    isEnPassant = true;
                    int capturedRow = startRow + (startPiece.isWhite() ? -1 : 1);
                    enPassantCapturedCell = board.getCell(capturedRow, endCol);
                    enPassantCapturedPawn = enPassantCapturedCell.getPiece();
                }
            }
        }

        // Detect castling move
        if (startPiece instanceof com.example.ChessGame.entity.piece.King) {
            int startRow = start.getRow();
            int startCol = start.getCol();
            int endCol = end.getCol();
            if (startRow == end.getRow() && Math.abs(startCol - endCol) == 2) {
                isCastling = true;
                int rookCol = endCol > startCol ? 7 : 0;
                int rookDestCol = endCol > startCol ? 5 : 3;
                castlingRookStart = board.getCell(startRow, rookCol);
                castlingRookEnd = board.getCell(startRow, rookDestCol);
                castlingRookPiece = castlingRookStart.getPiece();
            }
        }

        // Make the move simulation
        end.setPiece(startPiece);
        start.setPiece(null);
        if (isEnPassant && enPassantCapturedCell != null) {
            enPassantCapturedCell.setPiece(null);
        }
        if (isCastling && castlingRookStart != null && castlingRookEnd != null && castlingRookPiece != null) {
            castlingRookEnd.setPiece(castlingRookPiece);
            castlingRookStart.setPiece(null);
        }

        boolean isSafe = !isKingInCheck(isWhite);

        // Restore the board
        start.setPiece(startPiece);
        end.setPiece(endPiece);
        if (isEnPassant && enPassantCapturedCell != null) {
            enPassantCapturedCell.setPiece(enPassantCapturedPawn);
        }
        if (isCastling && castlingRookStart != null && castlingRookEnd != null) {
            castlingRookStart.setPiece(castlingRookPiece);
            castlingRookEnd.setPiece(null);
        }

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
