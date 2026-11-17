package com.gameconnect.notification.email;

import java.util.Objects;

/**
 * Provider-neutral email payload. Only the fields the notification foundation
 * needs today are modelled; templates and richer content are added later.
 */
public record EmailMessage(String to, String subject, String text, String html) {

    public EmailMessage {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        if ((text == null || text.isBlank()) && (html == null || html.isBlank())) {
            throw new IllegalArgumentException("either text or html body must be provided");
        }
    }
}
