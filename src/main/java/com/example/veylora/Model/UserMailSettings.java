package com.example.veylora.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A user's own sending mailbox (for example their Gmail). The password is stored encrypted, never as plain text. */
@Entity
@Table(name = "user_mail_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_mail_settings_user", columnNames = "userId"))
public class UserMailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 254)
    private String smtpEmail;

    @Column(nullable = false, length = 200)
    private String smtpHost = "smtp.gmail.com";

    @Column(nullable = false)
    private int smtpPort = 587;

    /** AES-GCM encrypted app password, base64. */
    @Column(nullable = false, length = 1000)
    private String passwordEnc;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSmtpEmail() { return smtpEmail; }
    public void setSmtpEmail(String smtpEmail) { this.smtpEmail = smtpEmail; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }
    public String getPasswordEnc() { return passwordEnc; }
    public void setPasswordEnc(String passwordEnc) { this.passwordEnc = passwordEnc; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
