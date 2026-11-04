package com.example.veylora.Model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** One person (phone number) in a campaign, and how far they got. */
@Entity
@Table(
    name = "campaign_recipients",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_campaign_recipient_phone", columnNames = {"campaign_id", "phoneNumber"})
)
public class CampaignRecipient {

    /**
     * PENDING = waiting to be sent, IN_PROGRESS = a task exists for the phone,
     * DONE = sent or call connected, FAILED = gave up after the allowed retries, CANCELLED = campaign stopped.
     */
    public enum RecipientStatus { PENDING, IN_PROGRESS, DONE, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Deleting a campaign also deletes its recipients (database-level cascade).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(length = 100)
    private String name;

    /** For EMAIL campaigns this holds a short key made from the address (the column is only 20 long); the real address is in email. */
    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(length = 254)
    private String email;

    /** The personal file (for example a certificate) matched to this person when the campaign was started. */
    private Long personalFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RecipientStatus status = RecipientStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    /** Do not try again before this time (used for retry delay). */
    private LocalDateTime nextAttemptAt;

    private LocalDateTime lastDispatchedAt;

    /** The device task made for the latest attempt. */
    private Long deviceTaskId;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Long getPersonalFileId() { return personalFileId; }
    public void setPersonalFileId(Long personalFileId) { this.personalFileId = personalFileId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public RecipientStatus getStatus() { return status; }
    public void setStatus(RecipientStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public LocalDateTime getLastDispatchedAt() { return lastDispatchedAt; }
    public void setLastDispatchedAt(LocalDateTime lastDispatchedAt) { this.lastDispatchedAt = lastDispatchedAt; }
    public Long getDeviceTaskId() { return deviceTaskId; }
    public void setDeviceTaskId(Long deviceTaskId) { this.deviceTaskId = deviceTaskId; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
