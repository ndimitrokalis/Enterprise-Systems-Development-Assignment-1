package org.example.guessinggame.controllers;

import jakarta.servlet.http.HttpSession;
import org.example.guessinggame.repositories.GameResultRepository;
import org.example.guessinggame.repositories.PlayerRepository;
import org.example.guessinggame.entities.GameResult;
import org.example.guessinggame.entities.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/game")
public class MainController {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameResultRepository gameResultRepository;

    private Random random = new Random();

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/start")
    public String startGame(@RequestParam("playerName") String playerName,
                            HttpSession session,
                            ModelMap model) {
        if (playerName == null || playerName.trim().isEmpty()) {
            model.addAttribute("error", "Please enter your name");
            return "index";
        }

        Player player = playerRepository.findByName(playerName.trim());

        if (player == null) {
            player = new Player(playerName.trim());
            playerRepository.save(player);
        }

        int targetNumber = random.nextInt(100) + 1;

        GameResult gameResult = new GameResult(player, targetNumber);
        gameResultRepository.save(gameResult);

        session.setAttribute("playerId", player.getId());
        session.setAttribute("playerName", playerName.trim());
        session.setAttribute("targetNumber", targetNumber);
        session.setAttribute("attempts", 0);
        session.setAttribute("gameActive", true);
        session.setAttribute("currentGameResultId", gameResult.getId());

        return "redirect:/game/play";
    }

    @GetMapping("/play")
    public String playGame(HttpSession session, ModelMap model) {
        Boolean gameActive = (Boolean) session.getAttribute("gameActive");

        if (gameActive == null || !gameActive) {
            return "redirect:/game/";
        }

        model.addAttribute("playerName", session.getAttribute("playerName"));
        model.addAttribute("attempts", session.getAttribute("attempts"));
        return "game";
    }

    @PostMapping("/guess")
    public String submitGuess(@RequestParam("guess") int guess,
                              HttpSession session,
                              ModelMap model) {
        Boolean gameActive = (Boolean) session.getAttribute("gameActive");

        if (gameActive == null || !gameActive) {
            return "redirect:/game/";
        }

        Integer attempts = (Integer) session.getAttribute("attempts");
        attempts++;
        session.setAttribute("attempts", attempts);

        Integer targetNumber = (Integer) session.getAttribute("targetNumber");
        Long gameResultId = (Long) session.getAttribute("currentGameResultId");

        GameResult gameResult = gameResultRepository.findById(gameResultId).get();
        gameResult.setAttempts(attempts);
        gameResultRepository.save(gameResult);

        model.addAttribute("playerName", session.getAttribute("playerName"));
        model.addAttribute("attempts", attempts);
        model.addAttribute("lastGuess", guess);

        if (guess == targetNumber) {
            gameResult.setCompleted(true);
            gameResult.setCompletedAt(LocalDateTime.now());
            gameResultRepository.save(gameResult);
            session.setAttribute("gameActive", false);

            model.addAttribute("message", "Correct! You won!");
            model.addAttribute("gameWon", true);
            model.addAttribute("targetNumber", targetNumber);
            return "game";
        } else if (guess < targetNumber) {
            model.addAttribute("message", "Too Low! Try a higher number.");
            model.addAttribute("feedback", "low");
        } else {
            model.addAttribute("message", "Too High! Try a lower number.");
            model.addAttribute("feedback", "high");
        }

        return "game";
    }

    @GetMapping("/history")
    public String viewHistory(HttpSession session, ModelMap model) {
        String playerName = (String) session.getAttribute("playerName");

        if (playerName == null) {
            return "redirect:/game/";
        }

        Player player = playerRepository.findByName(playerName);
        List<GameResult> history = gameResultRepository.findByPlayer(player);

        model.addAttribute("playerName", playerName);
        model.addAttribute("history", history);
        return "history";
    }

    @GetMapping("/leaderboard")
    public String viewLeaderboard(ModelMap model) {
        List<GameResult> leaderboard = gameResultRepository.findAll();
        model.addAttribute("leaderboard", leaderboard);
        return "leaderboard";
    }

    @GetMapping("/new")
    public String newGame(HttpSession session, RedirectAttributes redirectAttributes) {
        String playerName = (String) session.getAttribute("playerName");

        if (playerName != null) {
            Player player = playerRepository.findByName(playerName);

            int targetNumber = random.nextInt(100) + 1;

            GameResult gameResult = new GameResult(player, targetNumber);
            gameResultRepository.save(gameResult);

            session.setAttribute("targetNumber", targetNumber);
            session.setAttribute("attempts", 0);
            session.setAttribute("gameActive", true);
            session.setAttribute("currentGameResultId", gameResult.getId());
        }

        return "redirect:/game/play";
    }

    @GetMapping("/exit")
    public String exitGame(HttpSession session) {
        session.removeAttribute("playerId");
        session.removeAttribute("playerName");
        session.removeAttribute("targetNumber");
        session.removeAttribute("attempts");
        session.removeAttribute("gameActive");
        session.removeAttribute("currentGameResultId");

        return "redirect:/game/";
    }
}