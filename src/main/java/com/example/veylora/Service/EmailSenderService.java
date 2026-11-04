package com.example.veylora.Service;

import com.example.veylora.Model.CampaignAttachment;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/** Sends plain-text email (with optional PDF/image attachments) through the SMTP server set in .env. */
@Service
public class EmailSenderService {

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String host;
    private final String from;

    public EmailSenderService(ObjectProvider<JavaMailSender> senderProvider,
                              @Value("${spring.mail.host:}") String host,
                              @Value("${veylora.mail.from:${spring.mail.username:}}") String from) {
        this.senderProvider = senderProvider;
        this.host = host;
        this.from = from;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && senderProvider.getIfAvailable() != null;
    }

    public void send(String to, String subject, String body) {
        send(to, subject, body, List.of());
    }

    public void send(String to, String subject, String body, List<CampaignAttachment> attachments) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (!isConfigured() || sender == null) {
            throw new IllegalStateException(
                    "Email is not set up on the server. Set MAIL_HOST, MAIL_PORT, MAIL_USERNAME and MAIL_PASSWORD in .env.");
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, !attachments.isEmpty(), "UTF-8");
            if (from != null && !from.isBlank()) helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            for (CampaignAttachment a : attachments) {
                helper.addAttachment(a.getFileName(), new ByteArrayResource(a.getData()), a.getContentType());
            }
            sender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Could not build the email: " + e.getMessage(), e);
        }
    }
}
