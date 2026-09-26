package com.gameconnect.rating.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_rating")
public class PlayerRating {

    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "rater_id", nullable = false)
    private UUID raterId;

    @Column(name = "ratee_id", nullable = false)
    private UUID rateeId;

    @Column(nullable = false)
    private short score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public PlayerRating() {
    }

    public PlayerRating(UUID gameId, UUID raterId, UUID rateeId, int score) {
        this.gameId = gameId;
        this.raterId = raterId;
        this.rateeId = rateeId;
        this.score = (short) score;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public UUID getRaterId() {
        return raterId;
    }

    public UUID getRateeId() {
        return rateeId;
    }

    public short getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
