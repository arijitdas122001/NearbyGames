package com.gameconnect.game.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gameconnect.game.entity.JoinRequest;
import com.gameconnect.game.entity.JoinRequest.RequestStatus;

import jakarta.persistence.LockModeType;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

    Optional<JoinRequest> findByGameIdAndUserId(UUID gameId, UUID userId);

    boolean existsByGameIdAndUserIdAndStatus(UUID gameId, UUID userId, RequestStatus status);

    List<JoinRequest> findByGameIdOrderByCreatedAtAsc(UUID gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select jr from JoinRequest jr where jr.id = :id")
    Optional<JoinRequest> findByIdForUpdate(@Param("id") UUID id);
}