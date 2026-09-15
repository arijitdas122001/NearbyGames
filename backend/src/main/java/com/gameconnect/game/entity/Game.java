package com.gameconnect.game.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.gameconnect.auth.entity.User.SkillLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "game")
public class Game {

    public enum GameFormat {
        FIVE_V5("5V5"),
        SIX_V6("6V6"),
        SEVEN_V7("7V7"),
        EIGHT_V8("8V8"),
        NINE_V9("9V9"),
        ELEVEN_V11("11V11");

        private final String dbValue;

        GameFormat(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static GameFormat fromDbValue(String value) {
            if (value == null) {
                return null;
            }
            for (GameFormat format : values()) {
                if (format.dbValue.equals(value)) {
                    return format;
                }
            }
            return null;
        }

        @JsonValue
        public String toJson() {
            return dbValue;
        }

        @JsonCreator
        public static GameFormat fromJson(String value) {
            GameFormat format = fromDbValue(value);
            if (format == null) {
                throw new IllegalArgumentException("Unknown game format: " + value);
            }
            return format;
        }
    }

    public enum GameStatus {
        OPEN, FULL, IN_PROGRESS, COMPLETED, CANCELLED
    }

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "turf_name", nullable = false, length = 120)
    private String turfName;

    @Column(name = "turf_address", nullable = false, length = 255)
    private String turfAddress;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Convert(converter = GameFormatConverter.class)
    @Column(nullable = false, length = 10)
    private GameFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    private SkillLevel skillLevel;

    @Column(name = "maximum_players", nullable = false)
    private int maximumPlayers;

    @Column(name = "required_players")
    private Integer requiredPlayers;

    @Column(name = "joining_fee")
    private Integer joiningFee;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.OPEN;

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

    public Game() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getTurfName() {
        return turfName;
    }

    public void setTurfName(String turfName) {
        this.turfName = turfName;
    }

    public String getTurfAddress() {
        return turfAddress;
    }

    public void setTurfAddress(String turfAddress) {
        this.turfAddress = turfAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public void setGameDate(LocalDate gameDate) {
        this.gameDate = gameDate;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public GameFormat getFormat() {
        return format;
    }

    public void setFormat(GameFormat format) {
        this.format = format;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public int getMaximumPlayers() {
        return maximumPlayers;
    }

    public void setMaximumPlayers(int maximumPlayers) {
        this.maximumPlayers = maximumPlayers;
    }

    public Integer getRequiredPlayers() {
        return requiredPlayers;
    }

    public void setRequiredPlayers(Integer requiredPlayers) {
        this.requiredPlayers = requiredPlayers;
    }

    public Integer getJoiningFee() {
        return joiningFee;
    }

    public void setJoiningFee(Integer joiningFee) {
        this.joiningFee = joiningFee;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}