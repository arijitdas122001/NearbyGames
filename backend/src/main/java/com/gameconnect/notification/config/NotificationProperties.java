package com.gameconnect.notification.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@Component
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {

    private boolean schedulerEnabled = true;
    @NotNull
    private Duration pollInterval = Duration.ofSeconds(10);
    @Min(1)
    private int batchSize = 50;
    @Min(1)
    private int maxAttempts = 3;
    @NotNull
    private Duration baseRetryDelay = Duration.ofMinutes(1);
    @NotNull
    private Duration staleProcessingTimeout = Duration.ofMinutes(10);

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getBaseRetryDelay() {
        return baseRetryDelay;
    }

    public void setBaseRetryDelay(Duration baseRetryDelay) {
        this.baseRetryDelay = baseRetryDelay;
    }

    public Duration getStaleProcessingTimeout() {
        return staleProcessingTimeout;
    }

    public void setStaleProcessingTimeout(Duration staleProcessingTimeout) {
        this.staleProcessingTimeout = staleProcessingTimeout;
    }
}
