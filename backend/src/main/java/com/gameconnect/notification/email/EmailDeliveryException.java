package com.gameconnect.notification.email;

/**
 * Raised when an email could not be submitted to the provider. The provider
 * exception (if any) is always retained as the cause so callers can inspect the
 * underlying failure.
 */
public class EmailDeliveryException extends RuntimeException {

    private final boolean retryable;

    public EmailDeliveryException(String message) {
        this(message, null, false);
    }

    public EmailDeliveryException(String message, boolean retryable) {
        this(message, null, retryable);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        this(message, cause, true);
    }

    public EmailDeliveryException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
