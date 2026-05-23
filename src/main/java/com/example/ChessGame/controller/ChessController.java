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
    private final com.example.ChessGame.service.RoomService roomService;
    private final com.example.ChessGame.service.MessagingService messagingService;

    public ChessController(com.example.ChessGame.service.StockfishService stockfish, com.example.ChessGame.service.RoomService roomService, com.example.ChessGame.service.MessagingService messagingService) {
        this.stockfish = stockfish;
        this.roomService = roomService;
        this.messagingService = messagingService;
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

    @PostMapping("/rooms")
    public String createRoom(@RequestParam(required = false) String playerName, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        // validate the name
        String name = (playerName == null) ? "" : playerName.trim();
        if (name.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide a name to create a room.");
            return "redirect:/";
        }

        try {
            org.slf4j.LoggerFactory.getLogger(ChessController.class).info("Attempting to create room for name={}", name);
            String id = roomService.createRoom(name);
            // safe encoding using Charset overload
            String encodedName = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/room/" + id + "?name=" + encodedName;
        } catch (Exception e) {
            // log and surface friendly message
            org.slf4j.LoggerFactory.getLogger(ChessController.class).error("Failed to create room for name={}", name, e);
            redirectAttributes.addFlashAttribute("error", "Could not create room: " + e.getMessage());
            // preserve the submitted name so the user does not have to retype it
            redirectAttributes.addFlashAttribute("playerName", playerName);
            return "redirect:/";
        }
    }

    @GetMapping("/room/{roomId}")
    public String showRoom(@PathVariable String roomId, @RequestParam(required = false) String name, Model model) {
        model.addAttribute("roomId", roomId);
        model.addAttribute("playerName", name);
        // reuse the same game view template - the page will use WebSockets to subscribe to the room
        model.addAttribute("positions", generatePositions());

        // Provide safe defaults so Thymeleaf can render the template even before the game starts
        com.example.ChessGame.entity.Game roomGame = roomService.getGame(roomId);
        if (roomGame != null) {
            model.addAttribute("board", roomGame.getBoard());
            model.addAttribute("currentPlayer", roomGame.getCurrentTurn());
            model.addAttribute("gameStatus", roomGame.getGameStatus());
            model.addAttribute("whitePlayer", roomGame.getWhitePlayer());
            model.addAttribute("blackPlayer", roomGame.getBlackPlayer());
            model.addAttribute("capturedWhite", roomGame.getCapturedWhite());
            model.addAttribute("capturedBlack", roomGame.getCapturedBlack());

            // decide view orientation based on which player is viewing
            boolean viewAsWhite = (name != null && name.equals(roomGame.getWhitePlayer().getName()));
            model.addAttribute("viewAsWhite", viewAsWhite);

            // provide row indices according to view orientation so cells render correctly
            java.util.List<Integer> viewRowIndices = new java.util.ArrayList<>();
            if (viewAsWhite) {
                for (int i = 0; i < 8; i++) viewRowIndices.add(i);
            } else {
                for (int i = 7; i >= 0; i--) viewRowIndices.add(i);
            }
            model.addAttribute("viewRowIndices", viewRowIndices);
        } else {
            // placeholder values shown until opponent joins
            model.addAttribute("board", new com.example.ChessGame.entity.Board());
            model.addAttribute("currentPlayer", new com.example.ChessGame.entity.Player(name == null ? "Player" : name, true));
            model.addAttribute("gameStatus", com.example.ChessGame.entity.GameStatus.STARTED);
            model.addAttribute("whitePlayer", new com.example.ChessGame.entity.Player(name == null ? "Player" : name, true));
            model.addAttribute("blackPlayer", new com.example.ChessGame.entity.Player("Waiting...", false));
            model.addAttribute("capturedWhite", java.util.Collections.emptyList());
            model.addAttribute("capturedBlack", java.util.Collections.emptyList());
            model.addAttribute("viewAsWhite", true);
            java.util.List<Integer> viewRowIndices = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) viewRowIndices.add(i);
            model.addAttribute("viewRowIndices", viewRowIndices);
        }

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

    // REST join endpoint to ensure atomic joining for non-STOMP clients
    @PostMapping("/room/{roomId}/join")
    @ResponseBody
    public java.util.Map<String, Object> joinRoomAjax(@PathVariable String roomId, @RequestParam String playerName) {
        org.slf4j.LoggerFactory.getLogger(ChessController.class).info("REST join requested: roomId={}, playerName={}", roomId, playerName);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        try {
            String role = roomService.joinRoom(roomId, playerName);
            // allow idempotent rejoin by the creator (role == "white") — treat as success
            resp.put("success", true);
            resp.put("role", role);
            // broadcast updated state to any subscribed WS clients
            sendState(roomId);
            // also include the canonical state in the REST response so the client doesn't miss it
            resp.put("state", buildState(roomId));
            org.slf4j.LoggerFactory.getLogger(ChessController.class).info("REST join success/rejoin: roomId={}, playerName={}, role={}", roomId, playerName, role);
            return resp;
        } catch (IllegalArgumentException e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            org.slf4j.LoggerFactory.getLogger(ChessController.class).warn("REST join failed: room={}, name={}, error={}", roomId, playerName, e.getMessage());
            return resp;
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", "Unexpected error: " + e.getMessage());
            org.slf4j.LoggerFactory.getLogger(ChessController.class).error("REST join unexpected error: room={}, name={}, error={}", roomId, playerName, e.getMessage());
            return resp;
        }
    }

    @GetMapping("/room/{roomId}/debug")
    @ResponseBody
    public java.util.Map<String,Object> debugRoom(@PathVariable String roomId) {
        java.util.Map<String,Object> info = roomService.getRoomInfo(roomId);
        if (info == null) {
            return java.util.Map.of("found", false);
        }
        java.util.Map<String,Object> resp = new java.util.HashMap<>(info);
        resp.put("found", true);
        return resp;
    }

    // Build the MoveResponse state object for a room
    private com.example.ChessGame.controller.dto.MoveResponse buildState(String roomId) {
        com.example.ChessGame.entity.Room room = roomService.getRoom(roomId);
        com.example.ChessGame.entity.Game game = roomService.getGame(roomId);
        com.example.ChessGame.controller.dto.MoveResponse resp = new com.example.ChessGame.controller.dto.MoveResponse();
        if (game == null) {
            resp.setSuccess(false);
            resp.setMessage("Waiting for opponent or invalid room");
            if (room != null) {
                resp.setRoomStatus(room.getStatus().toString());
                resp.setPlayerCount(room.isFull() ? 2 : 1);
            }
            return resp;
        }

        java.util.List<java.util.List<com.example.ChessGame.controller.dto.MoveResponse.CellDto>> boardState = new java.util.ArrayList<>();
        for (int r = 0; r < 8; r++) {
            java.util.List<com.example.ChessGame.controller.dto.MoveResponse.CellDto> rowList = new java.util.ArrayList<>();
            for (int c = 0; c < 8; c++) {
                com.example.ChessGame.entity.Cell cell = game.getBoard().getCell(r, c);
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
        // include player names so clients can update UI when someone joins
        resp.setWhitePlayerName(game.getWhitePlayer().getName());
        resp.setBlackPlayerName(game.getBlackPlayer().getName());
        if (room != null) {
            resp.setRoomStatus(room.getStatus().toString());
            resp.setPlayerCount(room.isFull() ? 2 : 1);
        }
        return resp;
    }

    // Broadcast room state to WS subscribers (used by REST join and rest-move as well)
    private void sendState(String roomId) {
        com.example.ChessGame.controller.dto.MoveResponse resp = buildState(roomId);
        // use MessagingService for safe send
        this.messagingService.sendToTopic("/topic/rooms/" + roomId, resp);
    }

    @GetMapping("/room/{roomId}/state")
    @ResponseBody
    public com.example.ChessGame.controller.dto.MoveResponse getRoomState(@PathVariable String roomId) {
        return buildState(roomId);
    }

    @PostMapping("/room/{roomId}/move")
    @ResponseBody
    public java.util.Map<String,Object> moveInRoom(@PathVariable String roomId,
                                                   @RequestParam String from,
                                                   @RequestParam String to,
                                                   @RequestParam(required = false) String promotion,
                                                   @RequestParam String playerName) {
        java.util.Map<String,Object> resp = new java.util.HashMap<>();
        boolean ok = roomService.applyMove(roomId, from, to, promotion, playerName);
        if (ok) {
            resp.put("success", true);
            // broadcast updated state
            sendState(roomId);
        } else {
            resp.put("success", false);
            resp.put("message", "Invalid move or not your turn");
        }
        return resp;
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

    @GetMapping("/rooms/{roomId}/validMoves/{position}")
    @ResponseBody
    public List<String> getRoomValidMoves(@PathVariable String roomId, @PathVariable String position) {
        Game game = roomService.getGame(roomId);
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
