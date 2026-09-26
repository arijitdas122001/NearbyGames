package com.gameconnect.profile.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.profile.dto.PlayerStatsResponse;
import com.gameconnect.profile.dto.UpdateProfileRequest;
import com.gameconnect.profile.dto.UserProfileResponse;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PlayerStatsService playerStatsService;

    public ProfileService(UserRepository userRepository,
                          PlayerStatsService playerStatsService) {
        this.userRepository = userRepository;
        this.playerStatsService = playerStatsService;
    }

    public UserProfileResponse getProfile(UUID userId) {
        User user = findUser(userId);
        PlayerStatsResponse stats = computeStats(userId);
        return toResponse(user, stats);
    }

    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        if (request.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "At least one profile field must be provided");
        }

        User user = findUser(userId);

        if (request.displayName() != null) {
            String trimmed = request.displayName().trim();
            if (trimmed.isEmpty()) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "Display name must not be blank");
            }
            user.setDisplayName(trimmed);
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.profileImageUrl() != null) {
            user.setProfileImageUrl(request.profileImageUrl());
        }
        if (request.skillLevel() != null) {
            user.setSkillLevel(request.skillLevel());
        }
        if (request.position() != null) {
            user.setPosition(request.position());
        }

        userRepository.save(user);

        PlayerStatsResponse stats = computeStats(userId);
        return toResponse(user, stats);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"));
    }

    private PlayerStatsResponse computeStats(UUID userId) {
        return playerStatsService.getStats(userId);
    }

    private UserProfileResponse toResponse(User user, PlayerStatsResponse stats) {
        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfileImageUrl(),
                user.getSkillLevel(),
                user.getPosition(),
                user.getCreatedAt(),
                stats);
    }
}