package com.gameconnect.rating.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.rating.dto.CreatePlayerRatingRequest;
import com.gameconnect.rating.dto.GameRatingsResponse;
import com.gameconnect.rating.dto.PlayerRatingResponse;
import com.gameconnect.rating.service.PlayerRatingService;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games/{gameId}/ratings")
public class PlayerRatingController {

    private final PlayerRatingService playerRatingService;

    public PlayerRatingController(PlayerRatingService playerRatingService) {
        this.playerRatingService = playerRatingService;
    }

    @GetMapping({"", "/eligible"})
    public ResponseEntity<GameRatingsResponse> getEligibleRatings(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(playerRatingService.getEligibleRatings(principal, gameId));
    }

    @PostMapping
    public ResponseEntity<PlayerRatingResponse> createRating(
            @PathVariable UUID gameId,
            @Valid @RequestBody CreatePlayerRatingRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playerRatingService.createRating(principal, gameId, request));
    }
}
