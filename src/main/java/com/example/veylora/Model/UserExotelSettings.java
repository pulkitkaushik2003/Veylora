package com.example.veylora.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A user's own Exotel account (custom API). The API token is stored encrypted, never as plain text. */
@Entity
@Table(name = "user_exotel_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_exotel_settings_user", columnNames = "userId"))
public class UserExotelSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String accountSid;

    @Column(nullable = false, length = 200)
    private String apiKey;

    /** AES-GCM encrypted API token, base64. */
    @Column(nullable = false, length = 1000)
    private String apiTokenEnc;

    /** api.in.exotel.com (Mumbai) or api.exotel.com (Singapore). */
    @Column(nullable = false, length = 100)
    private String subdomain = "api.in.exotel.com";

    /** ExoPhone shown to the person being called. */
    @Column(length = 20)
    private String callerId;

    /** Sender (ExoPhone / registered sender id) for SMS. Falls back to callerId. */
    @Column(length = 20)
    private String smsFrom;

    /** India DLT principal entity id, sent with every SMS. */
    @Column(length = 60)
    private String dltEntityId;

    /** Exotel app (flow) used for calls when a campaign does not name one. */
    @Column(length = 30)
    private String defaultAppId;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccountSid() { return accountSid; }
    public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiTokenEnc() { return apiTokenEnc; }
    public void setApiTokenEnc(String apiTokenEnc) { this.apiTokenEnc = apiTokenEnc; }
    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }
    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }
    public String getSmsFrom() { return smsFrom; }
    public void setSmsFrom(String smsFrom) { this.smsFrom = smsFrom; }
    public String getDltEntityId() { return dltEntityId; }
    public void setDltEntityId(String dltEntityId) { this.dltEntityId = dltEntityId; }
    public String getDefaultAppId() { return defaultAppId; }
    public void setDefaultAppId(String defaultAppId) { this.defaultAppId = defaultAppId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
