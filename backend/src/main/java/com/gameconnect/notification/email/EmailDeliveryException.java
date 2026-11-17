package com.gameconnect.notification.email;

/**
 * Raised when an email could not be submitted to the provider. The provider
 * exception (if any) is always retained as the cause so callers can inspect the
 * underlying failure.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
