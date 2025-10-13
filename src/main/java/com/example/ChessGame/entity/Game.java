package com.example.ChessGame.entity;


import com.example.ChessGame.entity.piece.*;

import java.util.ArrayList;
import java.util.Scanner;

public class Game {
    Board board;
    Player whitePlayer;
    Player blackPlayer;
    Player currentTurn;
    GameStatus gameStatus;
    private final GameStateChecker gameStateChecker;
    ArrayList<Move> gameLog;

    public Game(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.currentTurn = whitePlayer;
        this.board = Board.getBoard();
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

        // Check if it's a capture move
        Piece capturedPiece = end.getPiece();
        if (capturedPiece != null) {
            capturedPiece.setKilled(true);
        }

        // Move the piece
        end.setPiece(start.getPiece());
        start.setPiece(null);

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

        // Make the move temporarily
        end.setPiece(startPiece);
        start.setPiece(null);

        // Check if the king is in check after this move
        boolean isSafe = !gameStateChecker.isKingInCheck(isWhite);

        // Restore the position
        start.setPiece(startPiece);
        end.setPiece(endPiece);

        return isSafe;
    }

    private void switchTurn() {
        currentTurn = (currentTurn == whitePlayer) ? blackPlayer : whitePlayer;
    }

    private Move getPlayerMove() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("Enter move (e.g., 'e2 e4' or 'resign'): ");
                String input = scanner.nextLine().trim().toLowerCase();

                if (input.equals("resign")) {
                    gameStatus = currentTurn.isWhite() ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
                    return null;
                }

                String[] parts = input.split(" ");
                if (parts.length != 2) {
                    System.out.println("Invalid input format. Please use format 'e2 e4'");
                    continue;
                }

                Cell start = getCell(parts[0]);
                Cell end = getCell(parts[1]);

                if (start == null || end == null) {
                    System.out.println("Invalid cell position!");
                    continue;
                }

                return new Move(start, end);
            } catch (Exception e) {
                System.out.println("Invalid input! Please try again.");
            }
        }
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

    private void displayBoard() {
        System.out.println("\n  a b c d e f g h");
        for (int i = 0; i < 8; i++) {
            System.out.print((8 - i) + " ");
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getCell(i, j).getPiece();
                if (piece == null) {
                    System.out.print(". ");
                } else {
                    char pieceChar = getPieceChar(piece);
                    System.out.print(piece.isWhite() ? Character.toUpperCase(pieceChar) : pieceChar);
                    System.out.print(" ");
                }
            }
            System.out.println(8 - i);
        }
        System.out.println("  a b c d e f g h");
    }

    private char getPieceChar(Piece piece) {
        if (piece instanceof Pawn) return 'p';
        if (piece instanceof Rook) return 'r';
        if (piece instanceof Knight) return 'n';
        if (piece instanceof Bishop) return 'b';
        if (piece instanceof Queen) return 'q';
        if (piece instanceof King) return 'k';
        return '?';
    }

    private void announceResult() {
        System.out.println("\nGame Over!");
        switch (gameStatus) {
            case WHITE_WIN:
                System.out.println(whitePlayer.getName() + " (White) wins!");
                break;
            case BLACK_WIN:
                System.out.println(blackPlayer.getName() + " (Black) wins!");
                break;
            case STALEMATE:
                System.out.println("Game ended in stalemate!");
                break;
            default:
                System.out.println("Game ended!");
        }
    }

    public Move createMove(String from, String to) {
        Cell startCell = getCell(from);
        Cell endCell = getCell(to);
        if (startCell != null && endCell != null) {
            return new Move(startCell, endCell);
        }
        return null;
    }

    public Board getBoard() {
        return board;
    }
}
