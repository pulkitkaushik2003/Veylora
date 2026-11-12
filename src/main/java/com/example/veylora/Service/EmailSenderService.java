package com.example.veylora.Service;

import com.example.veylora.Model.CampaignAttachment;
import com.example.veylora.Model.UserMailSettings;
import com.example.veylora.Repository.UserMailSettingsRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends email (with optional PDF/image attachments). If the user saved their own mailbox in Settings, the mail goes
 * out from it; otherwise it uses the server-wide mailbox from .env.
 */
@Service
public class EmailSenderService {

    private record Own(JavaMailSenderImpl sender, String from, java.time.LocalDateTime version) {}

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final UserMailSettingsRepository settingsRepository;
    private final MailCrypto crypto;
    private final String host;
    private final String from;
    private final Map<Long, Own> cache = new ConcurrentHashMap<>();

    public EmailSenderService(ObjectProvider<JavaMailSender> senderProvider,
                              UserMailSettingsRepository settingsRepository,
                              MailCrypto crypto,
                              @Value("${spring.mail.host:}") String host,
                              @Value("${veylora.mail.from:${spring.mail.username:}}") String from) {
        this.senderProvider = senderProvider;
        this.settingsRepository = settingsRepository;
        this.crypto = crypto;
        this.host = host;
        this.from = from;
    }

    /** The server-wide mailbox from .env. */
    public boolean isDefaultConfigured() {
        return host != null && !host.isBlank() && senderProvider.getIfAvailable() != null;
    }

    /** True when this user can send: they saved their own mailbox, or the server one is set. */
    public boolean isConfigured(Long userId) {
        return (userId != null && settingsRepository.findByUserId(userId).isPresent()) || isDefaultConfigured();
    }

    public boolean isConfigured() { return isDefaultConfigured(); }

    public void forget(Long userId) { cache.remove(userId); }

    public void send(String to, String subject, String body) {
        send(null, to, subject, body, List.of());
    }

    public void send(String to, String subject, String body, List<CampaignAttachment> attachments) {
        send(null, to, subject, body, attachments);
    }

    public void send(Long userId, String to, String subject, String body, List<CampaignAttachment> attachments) {
        JavaMailSender sender;
        String fromAddress;
        Own own = userId == null ? null : ownFor(userId);
        if (own != null) {
            sender = own.sender();
            fromAddress = own.from();
        } else {
            sender = senderProvider.getIfAvailable();
            fromAddress = from;
            if (!isDefaultConfigured() || sender == null) {
                throw new IllegalStateException(
                        "Email is not set up. Add your email and app password in Settings, or set MAIL_HOST, MAIL_PORT, MAIL_USERNAME and MAIL_PASSWORD in .env.");
            }
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, !attachments.isEmpty(), "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank()) helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            for (CampaignAttachment a : attachments) {
                helper.addAttachment(a.getFileName(), new ByteArrayResource(a.getData()), a.getContentType());
            }
            sender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not build the email: " + e.getMessage(), e);
        }
    }

    /** Logs in to the mail server once. Throws a readable error when the address or password is wrong. */
    public void verify(String host, int port, String email, String password) {
        JavaMailSenderImpl s = build(host, port, email, password);
        try {
            s.testConnection();
        } catch (MessagingException e) {
            String m = String.valueOf(e.getMessage());
            if (m.contains("535") || m.toLowerCase().contains("username and password not accepted")) {
                throw new IllegalArgumentException("The mail server rejected this email or password. For Gmail use a 16-character App Password (2-Step Verification must be on), not your normal password.");
            }
            throw new IllegalArgumentException("Could not connect to the mail server: " + m);
        }
    }

    private Own ownFor(Long userId) {
        Optional<UserMailSettings> opt = settingsRepository.findByUserId(userId);
        if (opt.isEmpty()) { cache.remove(userId); return null; }
        UserMailSettings s = opt.get();
        Own cached = cache.get(userId);
        if (cached != null && cached.version().equals(s.getUpdatedAt())) return cached;
        Own fresh = new Own(build(s.getSmtpHost(), s.getSmtpPort(), s.getSmtpEmail(), crypto.decrypt(s.getPasswordEnc())),
                s.getSmtpEmail(), s.getUpdatedAt());
        cache.put(userId, fresh);
        return fresh;
    }

    private static JavaMailSenderImpl build(String host, int port, String email, String password) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(host);
        s.setPort(port);
        s.setUsername(email);
        s.setPassword(password);
        s.setDefaultEncoding("UTF-8");
        java.util.Properties p = s.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        if (port == 465) p.put("mail.smtp.ssl.enable", "true"); else p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "15000");
        p.put("mail.smtp.writetimeout", "15000");
        return s;
    }
}
