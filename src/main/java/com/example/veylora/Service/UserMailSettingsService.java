package com.example.veylora.Service;

import com.example.veylora.Model.UserMailSettings;
import com.example.veylora.Repository.UserMailSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserMailSettingsService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final UserMailSettingsRepository repository;
    private final MailCrypto crypto;
    private final EmailSenderService sender;

    public UserMailSettingsService(UserMailSettingsRepository repository, MailCrypto crypto,
                                   EmailSenderService sender) {
        this.repository = repository;
        this.crypto = crypto;
        this.sender = sender;
    }

    public record View(boolean custom, String email, String host, int port, boolean defaultAvailable) {}

    @Transactional(readOnly = true)
    public View get(Long userId) {
        Optional<UserMailSettings> s = repository.findByUserId(userId);
        return s.map(v -> new View(true, v.getSmtpEmail(), v.getSmtpHost(), v.getSmtpPort(), sender.isDefaultConfigured()))
                .orElse(new View(false, null, null, 0, sender.isDefaultConfigured()));
    }

    /** Saves the user's mailbox. The password is checked against the mail server first, so a wrong one is never saved. */
    @Transactional
    public View save(Long userId, String email, String password, String host, Integer port) {
        String addr = email == null ? "" : email.trim();
        if (!EMAIL.matcher(addr).matches()) throw new IllegalArgumentException("Enter a valid email address");
        String h = host == null || host.isBlank() ? "smtp.gmail.com" : host.trim();
        int p = port == null ? 587 : port;
        if (p < 1 || p > 65535) throw new IllegalArgumentException("Invalid port");

        UserMailSettings s = repository.findByUserId(userId).orElseGet(UserMailSettings::new);
        String pass = password == null ? "" : password.replaceAll("\\s", "");
        if (pass.isEmpty()) {
            if (s.getId() == null) throw new IllegalArgumentException("Enter the app password");
            pass = crypto.decrypt(s.getPasswordEnc()); // keep the old password when only the email/host changed
        }
        sender.verify(h, p, addr, pass); // throws with a readable message when login fails

        s.setUserId(userId);
        s.setSmtpEmail(addr);
        s.setSmtpHost(h);
        s.setSmtpPort(p);
        s.setPasswordEnc(crypto.encrypt(pass));
        s.setUpdatedAt(LocalDateTime.now());
        repository.save(s);
        sender.forget(userId);
        return get(userId);
    }

    @Transactional
    public View remove(Long userId) {
        repository.deleteByUserId(userId);
        sender.forget(userId);
        return get(userId);
    }

    /** Sends a test mail from the user's own mailbox (or the default one) to the given address. */
    public void sendTest(Long userId, String to) {
        String addr = to == null ? "" : to.trim();
        if (!EMAIL.matcher(addr).matches()) throw new IllegalArgumentException("Enter a valid email address for the test");
        sender.send(userId, addr, "Veylora test email", "This is a test email from Veylora. Your mail settings work.", java.util.List.of());
    }
}
