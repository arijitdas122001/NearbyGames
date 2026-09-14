package com.gameconnect.profile.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.profile.dto.UpdateProfileRequest;
import com.gameconnect.profile.dto.UserProfileResponse;
import com.gameconnect.profile.service.ProfileService;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public UserProfileResponse getOwnProfile(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return profileService.getProfile(principal.id());
    }

    @GetMapping("/{id}")
    public UserProfileResponse getUserProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }

    @PatchMapping("/me")
    public UserProfileResponse updateOwnProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return profileService.updateProfile(principal.id(), request);
    }
}