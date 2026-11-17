package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.gameconnect.notification.config.EmailConfiguration;
import com.gameconnect.notification.email.EmailDeliveryException;
import com.gameconnect.notification.email.EmailMessage;
import com.gameconnect.notification.email.EmailProvider;
import com.gameconnect.notification.email.ResendEmailProvider;

class EmailConfigurationStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EmailConfiguration.class);

    @Test
    void startsWithoutEmailCredentials() {
        runner.withPropertyValues("mail.resend.api-key=", "mail.from=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EmailProvider.class);
                });
    }

    @Test
    void createsResendProviderWhenCredentialsArePresent() {
        runner.withPropertyValues("mail.resend.api-key=re_test_key", "mail.from=reminders@example.com")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(EmailProvider.class))
                            .isInstanceOf(ResendEmailProvider.class);
                });
    }

    @Test
    void sendingFailsClearlyWhenCredentialsAreMissing() {
        runner.withPropertyValues("mail.resend.api-key=", "mail.from=")
                .run(context -> {
                    EmailProvider provider = context.getBean(EmailProvider.class);

                    assertThatThrownBy(() -> provider.send(new EmailMessage(
                            "player@example.com", "Game reminder", "Body", null)))
                            .isInstanceOf(EmailDeliveryException.class)
                            .hasMessageContaining("not configured");
                });
    }
}
