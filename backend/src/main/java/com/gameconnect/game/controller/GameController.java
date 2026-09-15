package com.gameconnect.game.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.dto.GameResponse;
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
}