package at.pegelhub.notifications.transport;

import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties;
import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.DestinationConfig;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class SmtpDeliveryTransportTest {
    @Test
    void sendsToLocalSmtpServerAndDoesNotClaimRecipientReceipt() throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var executor = Executors.newSingleThreadExecutor()) {
            var accepted = executor.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(3000);

                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    var writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                    writer.print("220 localhost test\r\n");
                    writer.flush();

                    boolean data = false;
                    var message = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (data && !line.equals(".")) {
                            message.append(line).append('\n');
                            continue;
                        }

                        if (line.equals(".")) {
                            data = false;
                            writer.print("250 accepted\r\n");
                        } else if (line.startsWith("DATA")) {
                            data = true;
                            writer.print("354 continue\r\n");
                        } else if (line.startsWith("QUIT")) {
                            writer.print("221 bye\r\n");
                            writer.flush();
                            break;
                        } else {
                            writer.print("250 localhost\r\n");
                        }

                        writer.flush();
                    }

                    return message.toString();
                }
            });

            var credential = new NotificationProperties.Credential("SMTP", "localhost", server.getLocalPort(), false, false,
                    null, null, null, null, null, null, null, null, false);
            var catalog = new CredentialCatalog(new NotificationProperties(true, 90, Map.of("test", credential)));
            var route = new DestinationConfig("SMTP", true, DestinationConfig.Transport.SMTP, "test",
                    new DestinationConfig.MailRoute("sender@example.test", List.of("receiver@example.test"), "Signature"), null);
            var now = Instant.now();

            new SmtpDeliveryTransport(catalog).send(new Delivery(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), route,
                    "Test subject", "Test body", Delivery.State.SENDING, 1, now, now, null, null, UUID.randomUUID()));

            assertThat(accepted.get(3, TimeUnit.SECONDS))
                    .contains("Subject: Test subject", "Test body", "Signature", "receiver@example.test");
        }
    }

    @Test
    void rejectsHeaderInjectionAndUnknownCredentialWithoutNetworkCalls() {
        assertThatThrownBy(() -> new DestinationConfig.MailRoute(
                "sender@example.test\nBcc: evil@example.test", List.of("receiver@example.test"), null))
                .isInstanceOf(IllegalArgumentException.class);

        var config = new DestinationConfig("SMTP", true, DestinationConfig.Transport.SMTP, "missing",
                new DestinationConfig.MailRoute("sender@example.test", List.of("receiver@example.test"), null), null);

        assertThatThrownBy(() -> new CredentialCatalog(new NotificationProperties(false, 90, Map.of())).resolve(config))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
