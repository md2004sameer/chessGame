package com.example.ChessGame;

import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.PieceFactory;
import com.example.ChessGame.entity.Board;
import com.example.ChessGame.entity.Player;
import com.example.ChessGame.entity.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameFenAndPromotionTests {

    @Test
    public void testFenCastlingEnPassant() {
        Player w = new Player("W", true);
        Player b = new Player("B", false);
        Game g = new Game(w, b);
        g.startGame();
        // initial position should allow KQkq
        String fen = g.getFen();
        assertTrue(fen.contains("KQkq"));
        assertTrue(fen.contains("-"));
    }

    @Test
    public void testPromotionApplied() {
        Player w = new Player("W", true);
        Player b = new Player("B", false);
        Game g = new Game(w, b);
        g.startGame();
        Board board = g.getBoard();
        // Clear board and set a white pawn on e7
        for (int i=0;i<8;i++) for (int j=0;j<8;j++) board.getCell(i,j).setPiece(null);
        board.getCell(1,4).setPiece(PieceFactory.createPiece("pawn", true)); // row1 col4 is e7 (0-indexed)
        // create move e7 to e8 with promotion to queen
        Move m = g.createMove("e7","e8","q");
        g.makeMove(m, w);
        assertNotNull(board.getCell(0,4).getPiece());
        assertTrue(board.getCell(0,4).getPiece().getDisplayChar().equals("\u2655") || board.getCell(0,4).getPiece().getDisplayChar().equals("\u2655"));
    }
}
