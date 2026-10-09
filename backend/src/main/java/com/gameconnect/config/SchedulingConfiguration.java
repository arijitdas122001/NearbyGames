package com.gameconnect.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Registers Spring's scheduled-task infrastructure.
 *
 * <p>Tests set {@code app.scheduling.enabled=false} so the completion/start
 * timers never fire on their own; the scheduler methods are still beans and are
 * invoked directly by the test suite, which keeps every lifecycle assertion
 * deterministic.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfiguration {
}
