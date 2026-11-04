package com.example.veylora.Model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignType type = CampaignType.SMS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer totalRecipients = 0;

    @Column(nullable = false)
    private Integer successfulCount = 0;

    @Column(nullable = false)
    private Integer failedCount = 0;

    private Integer rateLimitPerMinute;

    private Integer maxRetries;

    private Integer retryDelayMinutes;

    private Boolean retryOnNoAnswer;

    @Column(length = 100)
    private String selectedTemplateId;

    @Column(length = 100)
    private String selectedAudioId;

    @Column(length = 100)
    private String selectedAiAgentId;

    @Column(length = 100)
    private String selectedDeviceId;

    private Integer selectedSimSlot;

    /** Text sent to every recipient of an SMS campaign. {{name}} and {{phone}} are filled in. */
    @Column(length = 1000)
    private String messageBody;

    /** Subject line of an EMAIL campaign. */
    @Column(length = 300)
    private String emailSubject;

    @Column(length = 150)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum CampaignType {
        SMS, EMAIL, CALL
    }

    public enum CampaignStatus {
        DRAFT, SCHEDULED, RUNNING, COMPLETED, PAUSED, FAILED
    }

    public Campaign() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public CampaignType getType() { return type; }
    public void setType(CampaignType type) { this.type = type; }

    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalRecipients() { return totalRecipients; }
    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public Integer getSuccessfulCount() { return successfulCount; }
    public void setSuccessfulCount(Integer successfulCount) {
        this.successfulCount = successfulCount;
    }

    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryDelayMinutes() { return retryDelayMinutes; }
    public void setRetryDelayMinutes(Integer retryDelayMinutes) {
        this.retryDelayMinutes = retryDelayMinutes;
    }

    public Boolean getRetryOnNoAnswer() { return retryOnNoAnswer; }
    public void setRetryOnNoAnswer(Boolean retryOnNoAnswer) {
        this.retryOnNoAnswer = retryOnNoAnswer;
    }

    public String getSelectedTemplateId() { return selectedTemplateId; }
    public void setSelectedTemplateId(String selectedTemplateId) {
        this.selectedTemplateId = selectedTemplateId;
    }

    public String getSelectedAudioId() { return selectedAudioId; }
    public void setSelectedAudioId(String selectedAudioId) {
        this.selectedAudioId = selectedAudioId;
    }

    public String getSelectedAiAgentId() { return selectedAiAgentId; }
    public void setSelectedAiAgentId(String selectedAiAgentId) {
        this.selectedAiAgentId = selectedAiAgentId;
    }

    public String getSelectedDeviceId() { return selectedDeviceId; }
    public void setSelectedDeviceId(String selectedDeviceId) {
        this.selectedDeviceId = selectedDeviceId;
    }

    public Integer getSelectedSimSlot() { return selectedSimSlot; }
    public void setSelectedSimSlot(Integer selectedSimSlot) {
        this.selectedSimSlot = selectedSimSlot;
    }

    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
