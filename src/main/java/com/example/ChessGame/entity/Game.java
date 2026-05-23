package com.example.ChessGame.entity;

import java.io.Serializable;

import java.util.ArrayList;


public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
    Board board;
    Player whitePlayer;
    Player blackPlayer;
    Player currentTurn;
    GameStatus gameStatus;
    private final GameStateChecker gameStateChecker;
    ArrayList<Move> gameLog;
    private java.util.List<Piece> capturedWhite = new java.util.ArrayList<>();
    private java.util.List<Piece> capturedBlack = new java.util.ArrayList<>();

    private boolean whiteKingMoved = false;
    private boolean whiteARookMoved = false; // a1 rook
    private boolean whiteHRookMoved = false; // h1 rook
    private boolean blackKingMoved = false;
    private boolean blackARookMoved = false; // a8 rook
    private boolean blackHRookMoved = false; // h8 rook
    private String enPassantTarget = "-"; // algebraic or '-'

    public Game(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.currentTurn = whitePlayer;
        this.board = new Board();
        this.gameStatus = GameStatus.STARTED;
        this.gameStateChecker = new GameStateChecker(board);
        this.gameLog = new ArrayList<>();
    }

    public Player getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void startGame() {
        board.initializeBoard();
        gameStatus = GameStatus.ACTIVE;
    }

    public void makeMove(Move move, Player player) {
        if (move == null || !isValidMove(move, player)) {
            return;
        }

        // Execute the move
        Cell start = move.getStart();
        Cell end = move.getEnd();

        // Check for en-passant capture first (special pawn capture)
        com.example.ChessGame.entity.Piece movingPiece = start.getPiece();
        if (movingPiece instanceof com.example.ChessGame.entity.piece.Pawn && isEnPassantMove(start, end)) {
            // captured pawn is on the square behind the en-passant target
            int capturedRow = end.getRow() + (movingPiece.isWhite() ? 1 : -1);
            Cell capturedCell = board.getCell(capturedRow, end.getCol());
            com.example.ChessGame.entity.Piece capturedPawn = capturedCell.getPiece();
            if (capturedPawn != null) {
                capturedPawn.setKilled(true);
                if (capturedPawn.isWhite()) capturedWhite.add(capturedPawn); else capturedBlack.add(capturedPawn);
                capturedCell.setPiece(null);
            }
        } else {
            // Normal capture (if any)
            com.example.ChessGame.entity.Piece capturedPiece = end.getPiece();
            if (capturedPiece != null) {
                capturedPiece.setKilled(true);
                if (capturedPiece.isWhite()) capturedWhite.add(capturedPiece); else capturedBlack.add(capturedPiece);
            }
        }

        // Move the piece to the destination
        end.setPiece(movingPiece);
        start.setPiece(null);

        // Handle castling: if the king moved two files, move the rook accordingly
        if (movingPiece instanceof com.example.ChessGame.entity.piece.King) {
            int startCol = start.getCol();
            int endCol = end.getCol();
            if (Math.abs(startCol - endCol) == 2) {
                // Determine rook start and end positions based on color and side
                Cell rookStart = null;
                Cell rookEnd = null;
                if (movingPiece.isWhite()) {
                    if (endCol == 6) { // white kingside castling (e1->g1)
                        rookStart = board.getCell(7,7);
                        rookEnd = board.getCell(7,5);
                    } else if (endCol == 2) { // white queenside (e1->c1)
                        rookStart = board.getCell(7,0);
                        rookEnd = board.getCell(7,3);
                    }
                } else {
                    if (endCol == 6) { // black kingside (e8->g8)
                        rookStart = board.getCell(0,7);
                        rookEnd = board.getCell(0,5);
                    } else if (endCol == 2) { // black queenside (e8->c8)
                        rookStart = board.getCell(0,0);
                        rookEnd = board.getCell(0,3);
                    }
                }
                if (rookStart != null && rookEnd != null) {
                    com.example.ChessGame.entity.Piece rookPiece = rookStart.getPiece();
                    if (rookPiece != null) {
                        rookEnd.setPiece(rookPiece);
                        rookStart.setPiece(null);
                        // mark the rook as moved
                        if (movingPiece.isWhite()) {
                            if (rookStart.getRow() == 7 && rookStart.getCol() == 7) whiteHRookMoved = true;
                            if (rookStart.getRow() == 7 && rookStart.getCol() == 0) whiteARookMoved = true;
                        } else {
                            if (rookStart.getRow() == 0 && rookStart.getCol() == 7) blackHRookMoved = true;
                            if (rookStart.getRow() == 0 && rookStart.getCol() == 0) blackARookMoved = true;
                        }
                    }
                }
            }
        }

        // Update castling/rook/king moved flags
        if (movingPiece instanceof com.example.ChessGame.entity.piece.King) {
            if (movingPiece.isWhite()) whiteKingMoved = true; else blackKingMoved = true;
        }
        if (movingPiece instanceof com.example.ChessGame.entity.piece.Rook) {
            if (movingPiece.isWhite()) {
                if (start.getRow() == 7 && start.getCol() == 0) whiteARookMoved = true;
                if (start.getRow() == 7 && start.getCol() == 7) whiteHRookMoved = true;
            } else {
                if (start.getRow() == 0 && start.getCol() == 0) blackARookMoved = true;
                if (start.getRow() == 0 && start.getCol() == 7) blackHRookMoved = true;
            }
        }

        // Update en-passant target: if a pawn moved two squares, set target square
        enPassantTarget = "-";
        if (movingPiece instanceof com.example.ChessGame.entity.piece.Pawn) {
            int startRow = start.getRow();
            int endRow = end.getRow();
            if (Math.abs(startRow - endRow) == 2) {
                // target is the square the pawn passed over
                int epRow = (startRow + endRow) / 2;
                int epCol = end.getCol();
                char file = (char) ('a' + epCol);
                int rank = 8 - epRow;
                enPassantTarget = String.format("%c%d", file, rank);
            }
        }

        // Handle promotion: if a pawn reached the last rank and promotion specified
        String promotion = move.getPromotion();
        if (promotion != null && !promotion.isEmpty()) {
            Piece moved = end.getPiece();
            if (moved instanceof com.example.ChessGame.entity.piece.Pawn) {
                // create promoted piece (default to queen)
                String promotedType = promotion.toLowerCase();
                String factoryType;
                switch (promotedType) {
                    case "q": case "queen": factoryType = "queen"; break;
                    case "r": case "rook": factoryType = "rook"; break;
                    case "b": case "bishop": factoryType = "bishop"; break;
                    case "n": case "knight": factoryType = "knight"; break;
                    default: factoryType = "queen"; break;
                }
                end.setPiece(PieceFactory.createPiece(factoryType, moved.isWhite()));
            }
        }

        // Log the move
        gameLog.add(move);

        // Check game ending conditions
        if (gameStateChecker.isCheckmate(!player.isWhite())) {
            gameStatus = player.isWhite() ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
            return;
        }

        if (gameStateChecker.isStalemate(!player.isWhite())) {
            gameStatus = GameStatus.STALEMATE;
            return;
        }

        // Switch turns
        switchTurn();
    }

    public java.util.List<Piece> getCapturedWhite() {
        return new java.util.ArrayList<>(capturedWhite);
    }

    public java.util.List<Piece> getCapturedBlack() {
        return new java.util.ArrayList<>(capturedBlack);
    }

    public void resignGame(Player player) {
        if (player != null) {
            gameStatus = player.isWhite() ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
        }
    }

    private boolean isValidMove(Move move, Player player) {
        if (gameStatus != GameStatus.ACTIVE) {
            return false;
        }

        if (player != currentTurn) {
            return false;
        }

        Cell start = move.getStart();
        Cell end = move.getEnd();

        if (start == null || end == null) {
            return false;
        }

        Piece piece = start.getPiece();

        if (piece == null || piece.isWhite() != player.isWhite()) {
            return false;
        }

        // Check if the move would leave or put the king in check
        if (!isMoveSafe(start, end, player.isWhite())) {
            return false;
        }

        return piece.canMove(board, start, end);
    }

    private boolean isMoveSafe(Cell start, Cell end, boolean isWhite) {
        // Store the current state
        Piece startPiece = start.getPiece();
        Piece endPiece = end.getPiece();

        // For en-passant we may need to temporarily remove the captured pawn
        boolean enPassant = false;
        Cell capturedCell = null;
        Piece capturedPawn = null;
        if (startPiece instanceof com.example.ChessGame.entity.piece.Pawn && isEnPassantMove(start, end)) {
            enPassant = true;
            int capturedRow = end.getRow() + (startPiece.isWhite() ? 1 : -1);
            capturedCell = board.getCell(capturedRow, end.getCol());
            capturedPawn = capturedCell.getPiece();
            // remove the captured pawn temporarily
            capturedCell.setPiece(null);
        }

        // Make the move temporarily
        end.setPiece(startPiece);
        start.setPiece(null);

        // Check if the king is in check after this move
        boolean isSafe = !gameStateChecker.isKingInCheck(isWhite);

        // Restore the position
        start.setPiece(startPiece);
        end.setPiece(endPiece);
        if (enPassant && capturedCell != null) {
            capturedCell.setPiece(capturedPawn);
        }

        return isSafe;
    }

    private void switchTurn() {
        currentTurn = (currentTurn == whitePlayer) ? blackPlayer : whitePlayer;
    }

    // Detect whether a pawn move from start to end represents an en-passant capture
    private boolean isEnPassantMove(Cell start, Cell end) {
        if (!(start.getPiece() instanceof com.example.ChessGame.entity.piece.Pawn)) return false;
        if (enPassantTarget == null || enPassantTarget.equals("-")) return false;
        String target = enPassantTarget;
        String endPos = String.format("%c%d", (char)('a' + end.getCol()), 8 - end.getRow());
        // en-passant capture ends on the enPassantTarget square
        return endPos.equals(target) && Math.abs(start.getCol() - end.getCol()) == 1 && Math.abs(start.getRow() - end.getRow()) == 1;
    }

    private Cell getCell(String position) {
        if (position.length() != 2) {
            return null;
        }

        int col = position.charAt(0) - 'a';
        int row = 8 - (position.charAt(1) - '0');

        if (row < 0 || row > 7 || col < 0 || col > 7) {
            return null;
        }

        return board.getCell(row, col);
    }

    public Move createMove(String from, String to) {
        Cell startCell = getCell(from);
        Cell endCell = getCell(to);
        if (startCell != null && endCell != null) {
            return new Move(startCell, endCell);
        }
        return null;
    }

    public Move createMove(String from, String to, String promotion) {
        Cell startCell = getCell(from);
        Cell endCell = getCell(to);
        if (startCell != null && endCell != null) {
            return new Move(startCell, endCell, promotion);
        }
        return null;
    }

    public Board getBoard() {
        return board;
    }

    public Player getWhitePlayer() {
        return whitePlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public void setWhitePlayer(Player whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public String getFen() {
        // Piece placement
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                com.example.ChessGame.entity.Piece p = board.getCell(row, col).getPiece();
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    char ch = '?';
                    if (p instanceof com.example.ChessGame.entity.piece.Pawn) ch = 'p';
                    else if (p instanceof com.example.ChessGame.entity.piece.Rook) ch = 'r';
                    else if (p instanceof com.example.ChessGame.entity.piece.Knight) ch = 'n';
                    else if (p instanceof com.example.ChessGame.entity.piece.Bishop) ch = 'b';
                    else if (p instanceof com.example.ChessGame.entity.piece.Queen) ch = 'q';
                    else if (p instanceof com.example.ChessGame.entity.piece.King) ch = 'k';
                    if (p.isWhite()) ch = Character.toUpperCase(ch);
                    sb.append(ch);
                }
            }
            if (empty > 0) sb.append(empty);
            if (row < 7) sb.append('/');
        }
        sb.append(' ');
        sb.append(currentTurn.isWhite() ? 'w' : 'b');
        sb.append(' ');

        // Castling rights
        StringBuilder castling = new StringBuilder();
        if (!whiteKingMoved) {
            if (!whiteHRookMoved) castling.append('K');
            if (!whiteARookMoved) castling.append('Q');
        }
        if (!blackKingMoved) {
            if (!blackHRookMoved) castling.append('k');
            if (!blackARookMoved) castling.append('q');
        }
        if (castling.length() == 0) sb.append('-'); else sb.append(castling);
        sb.append(' ');

        // En-passant target
        sb.append(enPassantTarget == null ? '-' : enPassantTarget);
        sb.append(' ');
        // halfmove clock and fullmove number omitted (use defaults)
        sb.append("0 1");
        return sb.toString();
    }

    public java.util.List<Move> getGameLog() {
        return new java.util.ArrayList<>(gameLog);
    }
}