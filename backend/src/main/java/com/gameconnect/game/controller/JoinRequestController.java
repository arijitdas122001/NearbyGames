package com.gameconnect.game.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.game.dto.DecisionRequest;
import com.gameconnect.game.dto.JoinRequestDetailResponse;
import com.gameconnect.game.dto.JoinRequestResponse;
import com.gameconnect.game.service.JoinRequestService;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games/{gameId}/join-requests")
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @PostMapping
    public ResponseEntity<JoinRequestResponse> createJoinRequest(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        JoinRequestResponse response = joinRequestService.createJoinRequest(principal, gameId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<JoinRequestResponse> getMyRequest(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(joinRequestService.getMyRequest(principal, gameId));
    }

    @GetMapping
    public ResponseEntity<List<JoinRequestDetailResponse>> getGameRequests(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(joinRequestService.getGameRequests(principal, gameId));
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<JoinRequestResponse> decideRequest(
            @PathVariable UUID gameId,
            @PathVariable UUID requestId,
            @Valid @RequestBody DecisionRequest decisionRequest,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        JoinRequestResponse response = joinRequestService.decideRequest(
                principal, gameId, requestId, decisionRequest.action());
        return ResponseEntity.ok(response);
    }
}