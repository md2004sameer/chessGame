package com.example.ChessGame.entity;

public class Board {
    Cell[][] cells;
    public static Board board;
    private Board() {
        cells = new Cell[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                cells[i][j] = new Cell(i, j, null, (i + j) % 2 != 0);
            }
        }
    }
    public static Board getBoard() {
        if (board == null) {
            board = new Board();
        }
        return board;
    }
    public boolean isCellEmpty(int x, int y) {
        return cells[x][y].getPiece() == null;
    }
    public Cell getCell(int x, int y) {
        return cells[x][y];
    }
    public void initializeBoard() {
        // Initialize White Pieces (Bottom of board - row 6,7)
        // Pawns
        for (int j = 0; j < 8; j++) {
            cells[6][j].setPiece(PieceFactory.createPiece("pawn", true));
        }

        // Back row pieces
        cells[7][0].setPiece(PieceFactory.createPiece("rook", true));
        cells[7][1].setPiece(PieceFactory.createPiece("knight", true));
        cells[7][2].setPiece(PieceFactory.createPiece("bishop", true));
        cells[7][3].setPiece(PieceFactory.createPiece("queen", true));
        cells[7][4].setPiece(PieceFactory.createPiece("king", true));
        cells[7][5].setPiece(PieceFactory.createPiece("bishop", true));
        cells[7][6].setPiece(PieceFactory.createPiece("knight", true));
        cells[7][7].setPiece(PieceFactory.createPiece("rook", true));

        // Initialize Black Pieces (Top of board - row 0,1)
        // Pawns
        for (int j = 0; j < 8; j++) {
            cells[1][j].setPiece(PieceFactory.createPiece("pawn", false));
        }

        // Back row pieces
        cells[0][0].setPiece(PieceFactory.createPiece("rook", false));
        cells[0][1].setPiece(PieceFactory.createPiece("knight", false));
        cells[0][2].setPiece(PieceFactory.createPiece("bishop", false));
        cells[0][3].setPiece(PieceFactory.createPiece("queen", false));
        cells[0][4].setPiece(PieceFactory.createPiece("king", false));
        cells[0][5].setPiece(PieceFactory.createPiece("bishop", false));
        cells[0][6].setPiece(PieceFactory.createPiece("knight", false));
        cells[0][7].setPiece(PieceFactory.createPiece("rook", false));
    }
}
