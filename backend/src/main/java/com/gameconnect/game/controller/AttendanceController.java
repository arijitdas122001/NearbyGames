package com.gameconnect.game.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.game.dto.AttendanceRequest;
import com.gameconnect.game.dto.ParticipantResponse;
import com.gameconnect.game.service.AttendanceService;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/games/{gameId}/participants")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(attendanceService.getParticipants(principal, gameId));
    }

    @PatchMapping("/{participantId}/attendance")
    public ResponseEntity<ParticipantResponse> updateAttendance(
            @PathVariable UUID gameId,
            @PathVariable UUID participantId,
            @Valid @RequestBody AttendanceRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        ParticipantResponse response = attendanceService.updateAttendance(
                principal, gameId, participantId, request.attended());
        return ResponseEntity.ok(response);
    }
}