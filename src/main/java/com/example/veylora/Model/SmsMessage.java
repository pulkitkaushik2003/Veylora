package com.example.veylora.Model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_messages")
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String recipientPhone;

    @Column(nullable = false, length = 5000)
    private String messageBody;

    @Column(length = 30)
    private String dltTemplateId;

    @Column(length = 20)
    private String dltHeader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsStatus status = SmsStatus.QUEUED;

    @Column(length = 150)
    private String providerMessageId;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum SmsStatus {
        QUEUED, SENT, DELIVERED, FAILED
    }

    public SmsMessage() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getDltTemplateId() { return dltTemplateId; }
    public void setDltTemplateId(String dltTemplateId) {
        this.dltTemplateId = dltTemplateId;
    }

    public String getDltHeader() { return dltHeader; }
    public void setDltHeader(String dltHeader) { this.dltHeader = dltHeader; }

    public SmsStatus getStatus() { return status; }
    public void setStatus(SmsStatus status) { this.status = status; }

    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
