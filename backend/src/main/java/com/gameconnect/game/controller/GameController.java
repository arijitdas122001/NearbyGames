package com.gameconnect.game.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.dto.GameDetailResponse;
import com.gameconnect.game.dto.GameResponse;
import com.gameconnect.game.dto.PagedGamesResponse;
import com.gameconnect.game.service.GameService;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @Valid @RequestBody CreateGameRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        GameResponse response = gameService.createGame(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedGamesResponse> listGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) SkillLevel skillLevel,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        PagedGamesResponse response = gameService.listOpenGames(
                page, size, date, format, skillLevel, q);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDetailResponse> getGame(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(gameService.getGameDetail(id));
    }
}