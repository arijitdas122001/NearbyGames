package com.gameconnect.rating.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gameconnect.rating.entity.PlayerRating;

public interface PlayerRatingRepository extends JpaRepository<PlayerRating, UUID> {

    boolean existsByGameIdAndRaterIdAndRateeId(UUID gameId, UUID raterId, UUID rateeId);

    List<PlayerRating> findByGameIdAndRaterId(UUID gameId, UUID raterId);
}
