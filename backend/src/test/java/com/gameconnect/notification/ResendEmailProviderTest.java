package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gameconnect.notification.email.EmailDeliveryException;
import com.gameconnect.notification.email.EmailMessage;
import com.gameconnect.notification.email.ResendEmailProvider;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@ExtendWith(MockitoExtension.class)
class ResendEmailProviderTest {

    private static final String FROM = "GameConnect <reminders@example.com>";

    @Mock
    private Emails emails;

    @Test
    void buildsRequestFromMessageAndReturnsProviderMessageId() throws ResendException {
        when(emails.send(any(CreateEmailOptions.class)))
                .thenReturn(new CreateEmailResponse("msg_123"));
        ResendEmailProvider provider = new ResendEmailProvider(emails, FROM);

        String messageId = provider.send(new EmailMessage(
                "player@example.com", "Game reminder", "Your game starts soon", null));

        assertThat(messageId).isEqualTo("msg_123");

        ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(emails).send(captor.capture());
        CreateEmailOptions options = captor.getValue();
        assertThat(options.getFrom()).isEqualTo(FROM);
        assertThat(options.getTo()).containsExactly("player@example.com");
        assertThat(options.getSubject()).isEqualTo("Game reminder");
        assertThat(options.getText()).isEqualTo("Your game starts soon");
        assertThat(options.getHtml()).isNull();
    }

    @Test
    void mapsProviderFailureAndKeepsUnderlyingCause() throws ResendException {
        ResendException providerError = new ResendException(422, "{\"message\":\"invalid from\"}");
        when(emails.send(any(CreateEmailOptions.class))).thenThrow(providerError);
        ResendEmailProvider provider = new ResendEmailProvider(emails, FROM);

        assertThatThrownBy(() -> provider.send(new EmailMessage(
                "player@example.com", "Game reminder", "Body", null)))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("Resend")
                .satisfies(error -> assertThat(error.getCause()).isSameAs(providerError));
    }

    @Test
    void failsWithoutCallingProviderWhenSenderIsMissing() {
        ResendEmailProvider provider = new ResendEmailProvider(emails, " ");

        assertThatThrownBy(() -> provider.send(new EmailMessage(
                "player@example.com", "Game reminder", "Body", null)))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("MAIL_FROM");

        verifyNoInteractions(emails);
    }
}
