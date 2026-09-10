package com.gameconnect.auth.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.auth.dto.AuthResponse;
import com.gameconnect.auth.dto.LoginRequest;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.auth.dto.UserResponse;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.auth.service.AuthService;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.gameconnect.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "auth_token";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final long cookieMaxAge;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          JwtService jwtService,
                          @Value("${jwt.expiration-minutes}") long cookieMaxAgeMinutes,
                          @Value("${cookie.secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.cookieMaxAge = cookieMaxAgeMinutes * 60;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        String token = jwtService.generateToken(response.id().toString(), response.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(token).toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        String token = jwtService.generateToken(response.id().toString(), response.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(token).toString())
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"));

        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfileImageUrl(),
<<<<<<< HEAD
                user.getSkillLevel(),
                user.getPosition());
=======
                user.getSkillLevel());
>>>>>>> f43f156 (phase 2 done, frontend set up and authentication)

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie clearedCookie = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }

    private ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .path("/")
                .maxAge(cookieMaxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();
    }
}
