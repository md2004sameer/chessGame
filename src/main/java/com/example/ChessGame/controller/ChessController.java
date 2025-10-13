package com.example.ChessGame.controller;

import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Move;
import com.example.ChessGame.entity.Player;
import com.example.ChessGame.entity.Cell;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChessController {
    private Game game;

    @GetMapping("/")
    public String showStartPage() {
        return "start";
    }

    @PostMapping("/start")
    public String startGame(@RequestParam String whitePlayerName,
                          @RequestParam String blackPlayerName) {
        Player whitePlayer = new Player(whitePlayerName, true);
        Player blackPlayer = new Player(blackPlayerName, false);
        game = new Game(whitePlayer, blackPlayer);
        game.startGame();
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String showGame(Model model) {
        if (game == null) {
            return "redirect:/";
        }
        model.addAttribute("board", game.getBoard());
        model.addAttribute("currentPlayer", game.getCurrentTurn());
        model.addAttribute("gameStatus", game.getGameStatus());
        model.addAttribute("positions", generatePositions());
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
    public String makeMove(@RequestParam String from, @RequestParam String to) {
        if (game != null) {
            Move move = game.createMove(from, to);
            if (move != null) {
                game.makeMove(move, game.getCurrentTurn());
            }
        }
        return "redirect:/game";
    }

    @PostMapping("/resign")
    public String resignGame() {
        if (game != null) {
            game.resignGame(game.getCurrentTurn());
        }
        return "redirect:/game";
    }

    @GetMapping("/validMoves/{position}")
    @ResponseBody
    public List<String> getValidMoves(@PathVariable String position) {
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
