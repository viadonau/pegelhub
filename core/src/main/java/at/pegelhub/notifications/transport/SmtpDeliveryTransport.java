package at.pegelhub.notifications.transport;

import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties.Credential;
import at.pegelhub.notifications.domain.Delivery;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class SmtpDeliveryTransport {

    private final CredentialCatalog credentials;

    public SmtpDeliveryTransport(CredentialCatalog credentials) {
        this.credentials = credentials;
    }

    /** Sends one attempt. Normal return means SMTP acceptance, not recipient receipt. */
    public void send(Delivery delivery) {
        var sender = createSender(credentials.resolve(delivery.route()));
        var route = delivery.route().mail();

        var message = new SimpleMailMessage();
        message.setFrom(route.from());
        message.setTo(route.recipients().toArray(String[]::new));
        message.setSubject(delivery.subject());
        message.setText(delivery.body()
                + (route.signature() == null || route.signature().isBlank() ? "" : "\n\n" + route.signature()));

        sender.send(message);
    }

    private JavaMailSenderImpl createSender(Credential credential) {
        var sender = new JavaMailSenderImpl();
        sender.setHost(credential.host());
        sender.setPort(credential.port());
        sender.setUsername(credential.username());
        sender.setPassword(credential.password());
        sender.setDefaultEncoding("UTF-8");

        var properties = sender.getJavaMailProperties();
        boolean startTls = credential.startTls() && !credential.ssl();
        properties.setProperty("mail.smtp.auth",
                Boolean.toString(credential.username() != null && !credential.username().isBlank()));

        properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.setProperty("mail.smtp.starttls.required", Boolean.toString(startTls));
        properties.setProperty("mail.smtp.ssl.enable", Boolean.toString(credential.ssl()));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");

        // Socket-operation limits, not a whole-attempt deadline: network I/O stays outside queue transactions.
        properties.setProperty("mail.smtp.connectiontimeout", "10000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");

        return sender;
    }
}
