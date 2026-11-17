package com.gameconnect.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gameconnect.notification.email.EmailDeliveryException;
import com.gameconnect.notification.email.EmailProvider;
import com.gameconnect.notification.email.ResendEmailProvider;
import com.resend.Resend;

/**
 * Wires the Resend-backed provider. A missing API key or sender is intentionally
 * not fatal: local development and the rest of the application must be able to
 * start without email credentials. Sending will fail with a clear exception
 * until both values are configured.
 */
@Configuration
public class EmailConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EmailConfiguration.class);

    @Bean
    public EmailProvider emailProvider(
            @Value("${mail.resend.api-key:}") String apiKey,
            @Value("${mail.from:}") String fromAddress) {
        boolean hasApiKey = apiKey != null && !apiKey.isBlank();
        boolean hasFrom = fromAddress != null && !fromAddress.isBlank();

        if (!hasApiKey) {
            log.warn("RESEND_API_KEY is not configured; email delivery is disabled.");
        }
        if (!hasFrom) {
            log.warn("MAIL_FROM is not configured; email delivery is disabled.");
        }

        if (!hasApiKey || !hasFrom) {
            return message -> {
                throw new EmailDeliveryException(
                        "Email delivery is not configured: set RESEND_API_KEY and MAIL_FROM");
            };
        }

        Resend resend = new Resend(apiKey);
        return new ResendEmailProvider(resend.emails(), fromAddress);
    }
}
