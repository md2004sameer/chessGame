package com.example.ChessGame.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import com.example.ChessGame.entity.Board;

import java.io.*;
import java.util.Locale;

/**
 * Minimal Stockfish runner that starts the engine process and uses UCI to get a bestmove.
 * Assumes 'stockfish' is available on PATH. For macOS users using Homebrew, install with
 * `brew install stockfish`.
 */
@Service
public class StockfishService implements Closeable {
    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;

    @PostConstruct
    public void startEngine() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("stockfish");
        pb.redirectErrorStream(true);
        process = pb.start();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        // initialize UCI
        sendCommand("uci");
        waitFor("uciok");
        sendCommand("isready");
        waitFor("readyok");
    }

    private void sendCommand(String cmd) throws IOException {
        stdin.write(cmd + "\n");
        stdin.flush();
    }

    private String readLine() throws IOException {
        return stdout.readLine();
    }

    private void waitFor(String token) throws IOException {
        String line;
        while ((line = readLine()) != null) {
            if (line.trim().toLowerCase(Locale.ROOT).contains(token)) {
                return;
            }
        }
    }

    /**
     * Ask stockfish for bestmove given the current board in FEN. This is a simple wrapper that
     * uses 'position fen {fen}' and 'go movetime {ms}' to request a move.
     */
    public String getBestMoveFromFen(String fen, int movetimeMs) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + movetimeMs);

        String line;
        String bestMove = null;
        while ((line = readLine()) != null) {
            line = line.trim();
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) bestMove = parts[1];
                break;
            }
        }
        return bestMove;
    }

    @Override
    public void close() throws IOException {
        if (process != null && process.isAlive()) {
            try { sendCommand("quit"); } catch (IOException ignored) {}
            try { stdin.close(); } catch (IOException ignored) {}
            try { stdout.close(); } catch (IOException ignored) {}
            process.destroy();
        }
    }

    @PreDestroy
    public void stopEngine() throws IOException {
        close();
    }

    // Helper: produce a FEN string from the Board. This project doesn't have one, so provide
    // a minimal implementation that iterates cells and composes a FEN without castling, en-passant,
    // move clocks. This is sufficient for Stockfish to play.
    public static String boardToFen(Board board, boolean whiteToMove) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                com.example.ChessGame.entity.Cell cell = board.getCell(row, col);
                com.example.ChessGame.entity.Piece p = cell.getPiece();
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
        sb.append(whiteToMove ? 'w' : 'b');
        // minimal: no castling rights, no en-passant, zero halfmove/fullmove counters
        sb.append(" - - 0 1");
        return sb.toString();
    }
}
