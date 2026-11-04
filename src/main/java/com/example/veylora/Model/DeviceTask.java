package com.example.veylora.Model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** A job the backend gives to a paired phone: send an SMS or place a call. */
@Entity
@Table(name = "device_tasks")
public class DeviceTask {

    public enum TaskType { SMS, CALL }

    /** PENDING -> CLAIMED (phone took it) -> DONE or FAILED. A pending task can be CANCELLED. */
    public enum TaskStatus { PENDING, CLAIMED, DONE, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Deleting a device also deletes its tasks (database-level cascade).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(length = 1000)
    private String messageBody;

    /** Which SIM slot to use (0 = first). Null = the phone's default SIM. */
    private Integer simSlotIndex;

    /** The sms_messages / call_logs row this task keeps up to date. */
    private Long smsMessageId;
    private Long callLogId;

    /** Set when this task was made for a campaign recipient. */
    private Long campaignRecipientId;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime claimedAt;
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
    public Integer getSimSlotIndex() { return simSlotIndex; }
    public void setSimSlotIndex(Integer simSlotIndex) { this.simSlotIndex = simSlotIndex; }
    public Long getSmsMessageId() { return smsMessageId; }
    public void setSmsMessageId(Long smsMessageId) { this.smsMessageId = smsMessageId; }
    public Long getCallLogId() { return callLogId; }
    public void setCallLogId(Long callLogId) { this.callLogId = callLogId; }
    public Long getCampaignRecipientId() { return campaignRecipientId; }
    public void setCampaignRecipientId(Long campaignRecipientId) { this.campaignRecipientId = campaignRecipientId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
