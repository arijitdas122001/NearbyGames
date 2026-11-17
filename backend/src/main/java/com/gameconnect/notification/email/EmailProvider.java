package com.gameconnect.notification.email;

/**
 * Small abstraction over the transactional email provider so notification
 * processing can be tested with a fake and the provider can be replaced without
 * touching domain code.
 */
public interface EmailProvider {

    /**
     * Submits an email for delivery.
     *
     * @return the provider-assigned message id
     * @throws EmailDeliveryException when the provider rejects the request or a
     *                                transport/configuration error occurs
     */
    String send(EmailMessage message);
}
