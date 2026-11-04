package com.example.veylora.Service;

import com.example.veylora.Exception.ResourceNotFoundException;
import com.example.veylora.Model.CallLog;
import com.example.veylora.Model.Device;
import com.example.veylora.Model.DeviceTask;
import com.example.veylora.Model.SmsMessage;
import com.example.veylora.Repository.DeviceTaskRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Queue of work for a paired phone. The user (logged in) creates tasks, the phone
 * (device token) claims them one at a time and reports the result. Each task keeps
 * the existing sms_messages / call_logs row up to date.
 */
@Service
public class DeviceTaskService {

    /** A claimed task whose result never arrives is failed (never retried: it could send twice). */
    private static final Duration SMS_STUCK_AFTER = Duration.ofMinutes(3);
    private static final Duration CALL_STUCK_AFTER = Duration.ofMinutes(45);

    private final DeviceTaskRepository taskRepository;
    private final DeviceService deviceService;
    private final SmsMessageService smsService;
    private final CallLogService callLogService;
    private final ApplicationEventPublisher events;

    public DeviceTaskService(DeviceTaskRepository taskRepository,
                             DeviceService deviceService,
                             SmsMessageService smsService,
                             CallLogService callLogService,
                             ApplicationEventPublisher events) {
        this.taskRepository = taskRepository;
        this.deviceService = deviceService;
        this.smsService = smsService;
        this.callLogService = callLogService;
        this.events = events;
    }

    // ---------------------------------------------------------------- user side

    @Transactional
    public DeviceTask create(Long userId, Long deviceId, DeviceTask.TaskType type,
                             String phoneNumber, String messageBody, Integer simSlotIndex) {

        Device device = deviceService.getDevice(userId, deviceId);
        if (device.getStatus() == Device.DeviceStatus.DISABLED) {
            throw new IllegalStateException("This device is disabled");
        }
        if (type == null) {
            throw new IllegalArgumentException("Task type is required");
        }

        String phone = phoneNumber == null ? "" : phoneNumber.trim().replaceAll("[\\s-]", "");
        if (!phone.matches("\\+?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Enter a valid phone number (digits, optional leading +)");
        }
        if (simSlotIndex != null && simSlotIndex < 0) {
            throw new IllegalArgumentException("SIM slot cannot be negative");
        }

        DeviceTask task = new DeviceTask();
        task.setUser(device.getUser());
        task.setDevice(device);
        task.setType(type);
        task.setPhoneNumber(phone);
        task.setSimSlotIndex(simSlotIndex);

        if (type == DeviceTask.TaskType.SMS) {
            if (messageBody == null || messageBody.isBlank()) {
                throw new IllegalArgumentException("Message text is required for an SMS");
            }
            task.setMessageBody(messageBody);
            SmsMessage sms = smsService.createSms(userId, phone, messageBody, null, null);
            task.setSmsMessageId(sms.getId());
        } else {
            CallLog log = new CallLog();
            log.setPhoneNumber(phone);
            log.setCallType(CallLog.CallType.OUTGOING);
            log.setStatus(CallLog.CallStatus.INITIATED);
            log.setDurationSeconds(0);
            task.setCallLogId(callLogService.createCallLog(userId, log).getId());
        }

        return taskRepository.save(task);
    }

    /** Same as create, but remembers which campaign recipient the task belongs to. */
    @Transactional
    public DeviceTask createForRecipient(Long userId, Long deviceId, DeviceTask.TaskType type,
                                         String phoneNumber, String messageBody, Integer simSlotIndex,
                                         Long recipientId) {
        DeviceTask task = create(userId, deviceId, type, phoneNumber, messageBody, simSlotIndex);
        task.setCampaignRecipientId(recipientId);
        return taskRepository.save(task);
    }

    /** Cancels a task only if the phone has not taken it yet. Returns true when it was cancelled. */
    @Transactional
    public boolean cancelIfPending(Long taskId) {
        DeviceTask task = taskRepository.lockById(taskId).orElse(null);
        if (task == null || task.getStatus() != DeviceTask.TaskStatus.PENDING) return false;
        task.setStatus(DeviceTask.TaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());
        syncHistory(task, false, "Cancelled", null, null);
        taskRepository.save(task);
        return true;
    }

    @Transactional(readOnly = true)
    public List<DeviceTask> list(Long userId, Long deviceId) {
        deviceService.getDevice(userId, deviceId); // ownership check
        return taskRepository.findTop50ByDevice_IdOrderByIdDesc(deviceId);
    }

    @Transactional
    public DeviceTask cancel(Long userId, Long deviceId, Long taskId) {
        deviceService.getDevice(userId, deviceId); // ownership check
        DeviceTask task = taskRepository.findByIdAndDevice_Id(taskId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.getStatus() != DeviceTask.TaskStatus.PENDING) {
            throw new IllegalStateException("Only a pending task can be cancelled");
        }
        task.setStatus(DeviceTask.TaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());
        syncHistory(task, false, "Cancelled", null, null);
        return taskRepository.save(task);
    }

    // -------------------------------------------------------------- device side

    /** Hands the oldest pending task to the phone and marks it CLAIMED. */
    @Transactional
    public Optional<DeviceTask> claimNext(Long deviceId) {
        failStuckTasks(deviceId);
        return taskRepository
                .findFirstByDevice_IdAndStatusOrderByIdAsc(deviceId, DeviceTask.TaskStatus.PENDING)
                .map(task -> {
                    task.setStatus(DeviceTask.TaskStatus.CLAIMED);
                    task.setClaimedAt(LocalDateTime.now());
                    return task;
                });
    }

    /**
     * The phone says what happened.
     * SMS: success = the phone's radio accepted every part of the message.
     * CALL: callOutcome is COMPLETED (connected, durationSeconds > 0), MISSED (not connected)
     *       or anything else/null when the phone could not tell.
     */
    @Transactional
    public DeviceTask report(Long deviceId, Long taskId, boolean success, String error,
                             Integer durationSeconds, String callOutcome) {
        DeviceTask task = taskRepository.findByIdAndDevice_Id(taskId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.getStatus() != DeviceTask.TaskStatus.CLAIMED) {
            throw new IllegalStateException("Task is not in progress");
        }

        String cleanError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        Integer duration = (durationSeconds == null || durationSeconds < 0) ? null : durationSeconds;
        task.setStatus(success ? DeviceTask.TaskStatus.DONE : DeviceTask.TaskStatus.FAILED);
        task.setErrorMessage(success ? null : cleanError);
        task.setCompletedAt(LocalDateTime.now());
        syncHistory(task, success, cleanError, duration, callOutcome);
        DeviceTask saved = taskRepository.save(task);
        publishFinished(saved, success, callOutcome, true, cleanError);
        return saved;
    }

    // ------------------------------------------------------------------ helpers

    private void failStuckTasks(Long deviceId) {
        LocalDateTime now = LocalDateTime.now();
        for (DeviceTask task : taskRepository.findByDevice_IdAndStatus(deviceId, DeviceTask.TaskStatus.CLAIMED)) {
            Duration limit = task.getType() == DeviceTask.TaskType.SMS ? SMS_STUCK_AFTER : CALL_STUCK_AFTER;
            if (task.getClaimedAt() != null && task.getClaimedAt().plus(limit).isBefore(now)) {
                task.setStatus(DeviceTask.TaskStatus.FAILED);
                task.setErrorMessage("The phone did not report a result in time.");
                task.setCompletedAt(now);
                syncHistory(task, false, task.getErrorMessage(), null, null);
                // Unclear whether it was sent, so a campaign must not retry it automatically.
                publishFinished(task, false, null, false, task.getErrorMessage());
            }
        }
    }

    private void publishFinished(DeviceTask task, boolean success, String callOutcome,
                                 boolean retryable, String error) {
        if (task.getCampaignRecipientId() != null) {
            events.publishEvent(new TaskFinishedEvent(task.getId(), task.getCampaignRecipientId(),
                    task.getType(), success, callOutcome, retryable, error));
        }
    }

    private void syncHistory(DeviceTask task, boolean success, String error,
                             Integer durationSeconds, String callOutcome) {
        Long userId = task.getUser().getId();
        if (task.getType() == DeviceTask.TaskType.SMS && task.getSmsMessageId() != null) {
            smsService.updateSmsStatus(userId, task.getSmsMessageId(),
                    success ? SmsMessage.SmsStatus.SENT : SmsMessage.SmsStatus.FAILED,
                    null, success ? null : error);
        } else if (task.getType() == DeviceTask.TaskType.CALL && task.getCallLogId() != null) {
            CallLog.CallStatus status;
            if (!success) {
                status = CallLog.CallStatus.FAILED;
            } else if ("COMPLETED".equals(callOutcome)) {
                status = CallLog.CallStatus.COMPLETED;
            } else if ("MISSED".equals(callOutcome)) {
                status = CallLog.CallStatus.MISSED;
            } else {
                status = CallLog.CallStatus.INITIATED; // dial started, outcome unknown
            }
            callLogService.updateCallStatus(userId, task.getCallLogId(), status, durationSeconds);
        }
    }
}
