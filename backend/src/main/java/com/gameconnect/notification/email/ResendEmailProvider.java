package com.gameconnect.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

/**
 * {@link EmailProvider} backed by the official Resend Java SDK.
 *
 * <p>Only non-sensitive metadata is logged. Message bodies, recipients and the
 * API key are never written to logs.
 */
public class ResendEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);

    private final Emails emails;
    private final String fromAddress;

    public ResendEmailProvider(Emails emails, String fromAddress) {
        this.emails = emails;
        this.fromAddress = fromAddress;
    }

    @Override
    public String send(EmailMessage message) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new EmailDeliveryException("MAIL_FROM is not configured");
        }

        CreateEmailOptions.Builder options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(message.to())
                .subject(message.subject());

        if (message.text() != null && !message.text().isBlank()) {
            options.text(message.text());
        }
        if (message.html() != null && !message.html().isBlank()) {
            options.html(message.html());
        }

        try {
            CreateEmailResponse response = emails.send(options.build());
            if (response == null || response.getId() == null || response.getId().isBlank()) {
                throw new EmailDeliveryException("Resend returned an empty message id");
            }
            return response.getId();
        } catch (ResendException e) {
            log.warn("Resend email delivery failed (status={}, error={})",
                    e.getStatusCode(), e.getErrorName());
            throw new EmailDeliveryException("Resend email delivery failed", e);
        }
    }
}
