package com.example.ChessGame.controller;

import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Move;
import com.example.ChessGame.entity.Player;
import com.example.ChessGame.entity.Cell;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChessController {
    // per-session game state will be stored in HttpSession; no controller-level shared game/flags
    private final com.example.ChessGame.service.StockfishService stockfish;

    public ChessController(com.example.ChessGame.service.StockfishService stockfish) {
        this.stockfish = stockfish;
    }

    @GetMapping("/")
    public String showStartPage() {
        return "start";
    }

    @PostMapping("/start")
    public String startGame(@RequestParam String whitePlayerName,
                            @RequestParam String blackPlayerName,
                            @RequestParam(required = false) String whiteAi,
                            @RequestParam(required = false) String blackAi,
                            HttpSession session) throws Exception {
        Player whitePlayer = new Player(whitePlayerName, true);
        Player blackPlayer = new Player(blackPlayerName, false);
        Game game = new Game(whitePlayer, blackPlayer);
        game.startGame();
        boolean whiteIsAI = (whiteAi != null && (whiteAi.equalsIgnoreCase("on") || whiteAi.equalsIgnoreCase("true") || whiteAi.equalsIgnoreCase("yes")));
        boolean blackIsAI = (blackAi != null && (blackAi.equalsIgnoreCase("on") || blackAi.equalsIgnoreCase("true") || blackAi.equalsIgnoreCase("yes")));
        // store per-session
        session.setAttribute("game", game);
        session.setAttribute("whiteIsAI", whiteIsAI);
        session.setAttribute("blackIsAI", blackIsAI);
        // Stockfish is a spring-managed bean; no need to instantiate here
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String showGame(Model model, HttpSession session) {
        Game game = (Game) session.getAttribute("game");
        if (game == null) {
            return "redirect:/";
        }
        model.addAttribute("board", game.getBoard());
        model.addAttribute("currentPlayer", game.getCurrentTurn());
        model.addAttribute("gameStatus", game.getGameStatus());
        // expose which side(s) are controlled by AI so the template can indicate it
        boolean whiteIsAI = Boolean.TRUE.equals(session.getAttribute("whiteIsAI"));
        boolean blackIsAI = Boolean.TRUE.equals(session.getAttribute("blackIsAI"));
        model.addAttribute("whiteIsAI", whiteIsAI);
        model.addAttribute("blackIsAI", blackIsAI);
        model.addAttribute("whitePlayer", game.getWhitePlayer());
        model.addAttribute("blackPlayer", game.getBlackPlayer());

        // Decide default view orientation: prefer showing the human player at the bottom.
        boolean viewAsWhite;
        if (!whiteIsAI && blackIsAI) {
            // human is white
            viewAsWhite = true;
        } else if (whiteIsAI && !blackIsAI) {
            // human is black
            viewAsWhite = false;
        } else {
            // both human or both AI - default to white at bottom
            viewAsWhite = true;
        }
        model.addAttribute("viewAsWhite", viewAsWhite);

        // provide a list of board row indices in the order they should be rendered
        java.util.List<Integer> viewRowIndices = new java.util.ArrayList<>();
        if (viewAsWhite) {
            for (int i = 0; i < 8; i++) viewRowIndices.add(i);
        } else {
            for (int i = 7; i >= 0; i--) viewRowIndices.add(i);
        }
        model.addAttribute("viewRowIndices", viewRowIndices);

        model.addAttribute("positions", generatePositions());
        // expose captured pieces for display
        model.addAttribute("capturedWhite", game.getCapturedWhite());
        model.addAttribute("capturedBlack", game.getCapturedBlack());
        return "game";
    }

    private List<List<String>> generatePositions() {
        List<List<String>> positions = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            List<String> rowPositions = new ArrayList<>();
            for (int col = 0; col < 8; col++) {
                char file = (char) ('a' + col);
                int rank = 8 - row;
                rowPositions.add(String.format("%c%d", file, rank));
            }
            positions.add(rowPositions);
        }
        return positions;
    }

    @PostMapping("/move")
    public String makeMove(@RequestParam String from, @RequestParam String to,
                           @RequestParam(required = false) String promotion,
                           HttpSession session) {
        // Keep existing behavior for non-AJAX/form requests: perform move and redirect back to /game
        applyMoveWithPossibleAi(session, from, to, promotion);
        return "redirect:/game";
    }

    @PostMapping(value = "/move", produces = "application/json")
    @ResponseBody
    public com.example.ChessGame.controller.dto.MoveResponse makeMoveAjax(@RequestParam String from, @RequestParam String to,
                                                      @RequestParam(required = false) String promotion,
                                                      HttpSession session) {
        // AJAX JSON response: apply move (and AI response if any) and return a compact board state
        applyMoveWithPossibleAi(session, from, to, promotion);

        com.example.ChessGame.controller.dto.MoveResponse resp = new com.example.ChessGame.controller.dto.MoveResponse();
        Game game = (Game) session.getAttribute("game");
        if (game == null) {
            resp.setSuccess(false);
            resp.setMessage("No game in progress");
            return resp;
        }

        java.util.List<java.util.List<com.example.ChessGame.controller.dto.MoveResponse.CellDto>> boardState = new java.util.ArrayList<>();
        for (int r = 0; r < 8; r++) {
            java.util.List<com.example.ChessGame.controller.dto.MoveResponse.CellDto> rowList = new java.util.ArrayList<>();
            for (int c = 0; c < 8; c++) {
                Cell cell = game.getBoard().getCell(r, c);
                com.example.ChessGame.controller.dto.MoveResponse.CellDto cellDto = new com.example.ChessGame.controller.dto.MoveResponse.CellDto();
                char file = (char) ('a' + c);
                int rank = 8 - r;
                String pos = String.format("%c%d", file, rank);
                cellDto.position = pos;
                if (cell.getPiece() != null) {
                    cellDto.display = cell.getPiece().getDisplayChar();
                    cellDto.type = cell.getPiece().getType();
                    cellDto.color = cell.getPiece().isWhite();
                } else {
                    cellDto.display = "";
                    cellDto.type = "";
                    cellDto.color = "";
                }
                rowList.add(cellDto);
            }
            boardState.add(rowList);
        }

        resp.setSuccess(true);
        resp.setBoard(boardState);
        resp.setCurrentPlayer(game.getCurrentTurn().getName());
        resp.setGameStatus(game.getGameStatus().toString());
        return resp;
    }

    private void applyMoveWithPossibleAi(HttpSession session, String from, String to, String promotion) {
        Game game = (Game) session.getAttribute("game");
        if (game != null) {
            Move move = (promotion == null) ? game.createMove(from, to) : game.createMove(from, to, promotion);
            if (move != null) {
                game.makeMove(move, game.getCurrentTurn());
                // If either side is AI and it's now that side's turn, ask stockfish for a move
                try {
                    boolean whiteIsAI = Boolean.TRUE.equals(session.getAttribute("whiteIsAI"));
                    boolean blackIsAI = Boolean.TRUE.equals(session.getAttribute("blackIsAI"));
                    boolean nowWhite = game.getCurrentTurn().isWhite();
                    if ((nowWhite && whiteIsAI) || (!nowWhite && blackIsAI)) {
                        String fen = game.getFen();
                        String best = stockfish.getBestMoveFromFen(fen, 200);
                        if (best != null && best.length() >= 4) {
                            String fromAi = best.substring(0,2);
                            String toAi = best.substring(2,4);
                            String promo = null;
                            if (best.length() >= 5) promo = String.valueOf(best.charAt(4));
                            Move aiMove = (promo == null) ? game.createMove(fromAi, toAi) : game.createMove(fromAi, toAi, promo);
                            if (aiMove != null) game.makeMove(aiMove, game.getCurrentTurn());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // persist back
        session.setAttribute("game", game);
    }

    @PostMapping("/resign")
    public String resignGame(HttpSession session) {
        Game game = (Game) session.getAttribute("game");
        if (game != null) {
            game.resignGame(game.getCurrentTurn());
            session.setAttribute("game", game);
        }
        return "redirect:/game";
    }

    @PostMapping("/exit")
    public String exitGame(HttpSession session) {
        // Reset the game to null so a fresh game is started on new /start
        // If you want to preserve player names but reset the board, create a new Game with same players.
        Game game = (Game) session.getAttribute("game");
        if (game != null) {
            try {
                Player white = game.getWhitePlayer();
                Player black = game.getBlackPlayer();
                // create a fresh game with same players (per-Game Board instance will be new)
                game = new Game(white, black);
                game.startGame();
                session.setAttribute("game", game);
                session.removeAttribute("whiteIsAI");
                session.removeAttribute("blackIsAI");
            } catch (Exception e) {
                // fallback: clear reference
                session.removeAttribute("game");
            }
        }
        return "redirect:/";
    }

    @GetMapping("/validMoves/{position}")
    @ResponseBody
    public List<String> getValidMoves(@PathVariable String position, HttpSession session) {
        Game game = (Game) session.getAttribute("game");
        if (game == null) {
            return new ArrayList<>();
        }

        int row = 8 - Character.getNumericValue(position.charAt(1));
        int col = position.charAt(0) - 'a';
        Cell start = game.getBoard().getCell(row, col);

        if (start.getPiece() == null || start.getPiece().isWhite() != game.getCurrentTurn().isWhite()) {
            return new ArrayList<>();
        }

        List<String> validMoves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Cell end = game.getBoard().getCell(r, c);
                if (start.getPiece().canMove(game.getBoard(), start, end)) {
                    char file = (char) ('a' + c);
                    int rank = 8 - r;
                    validMoves.add(String.format("%c%d", file, rank));
                }
            }
        }
        return validMoves;
    }
}
