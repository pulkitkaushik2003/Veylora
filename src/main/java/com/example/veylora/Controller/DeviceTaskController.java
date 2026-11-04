package com.example.veylora.Controller;

import com.example.veylora.Model.DeviceTask;
import com.example.veylora.Service.DeviceTaskService;
import com.example.veylora.config.CurrentUserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User side (logged in, session):   /api/v1/devices/{deviceId}/tasks
 * Phone side (X-Device-Token):      /api/v1/device-agent/tasks/...
 */
@RestController
public class DeviceTaskController {

    private final DeviceTaskService taskService;

    public DeviceTaskController(DeviceTaskService taskService) {
        this.taskService = taskService;
    }

    // ---------------------------------------------------------------- user side

    @PostMapping("/api/v1/devices/{deviceId}/tasks")
    public ResponseEntity<TaskResponse> create(@CurrentUserId Long userId,
                                               @PathVariable Long deviceId,
                                               @Valid @RequestBody CreateTaskRequest request) {
        DeviceTask task = taskService.create(userId, deviceId, request.type(),
                request.phoneNumber(), request.messageBody(), request.simSlotIndex());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(task));
    }

    @GetMapping("/api/v1/devices/{deviceId}/tasks")
    public List<TaskResponse> list(@CurrentUserId Long userId, @PathVariable Long deviceId) {
        return taskService.list(userId, deviceId).stream().map(DeviceTaskController::toResponse).toList();
    }

    @PostMapping("/api/v1/devices/{deviceId}/tasks/{taskId}/cancel")
    public TaskResponse cancel(@CurrentUserId Long userId,
                               @PathVariable Long deviceId,
                               @PathVariable Long taskId) {
        return toResponse(taskService.cancel(userId, deviceId, taskId));
    }

    // ---------------------------------------------------------------- phone side

    /** 200 with the next task, or 204 when there is nothing to do. */
    @GetMapping("/api/v1/device-agent/tasks/next")
    public ResponseEntity<AgentTask> next(@AuthenticationPrincipal Long deviceId) {
        return taskService.claimNext(deviceId)
                .map(t -> ResponseEntity.ok(new AgentTask(
                        t.getId(), t.getType(), t.getPhoneNumber(), t.getMessageBody(), t.getSimSlotIndex())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/api/v1/device-agent/tasks/{taskId}/result")
    public TaskResponse result(@AuthenticationPrincipal Long deviceId,
                               @PathVariable Long taskId,
                               @Valid @RequestBody ResultRequest request) {
        return toResponse(taskService.report(deviceId, taskId, request.success(),
                request.errorMessage(), request.durationSeconds(), request.callOutcome()));
    }

    // ------------------------------------------------------------------- shapes

    private static TaskResponse toResponse(DeviceTask t) {
        return new TaskResponse(t.getId(), t.getType(), t.getStatus(), t.getPhoneNumber(),
                t.getMessageBody(), t.getSimSlotIndex(), t.getErrorMessage(),
                t.getCreatedAt(), t.getCompletedAt());
    }

    public record CreateTaskRequest(
            @NotNull DeviceTask.TaskType type,
            @NotBlank @Size(max = 30) String phoneNumber,
            @Size(max = 1000) String messageBody,
            Integer simSlotIndex) {}

    public record ResultRequest(
            boolean success,
            @Size(max = 500) String errorMessage,
            Integer durationSeconds,
            @Size(max = 20) String callOutcome) {}

    public record AgentTask(
            Long id, DeviceTask.TaskType type, String phoneNumber,
            String messageBody, Integer simSlotIndex) {}

    public record TaskResponse(
            Long id, DeviceTask.TaskType type, DeviceTask.TaskStatus status,
            String phoneNumber, String messageBody, Integer simSlotIndex,
            String errorMessage, LocalDateTime createdAt, LocalDateTime completedAt) {}
}