package com.example.veylora.Controller;


import com.example.veylora.config.CurrentUserId;
import com.example.veylora.Model.CallLog;
import com.example.veylora.Service.CallLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/call-logs")
public class CallLogController {

    private final CallLogService callLogService;

    public CallLogController(CallLogService callLogService) {
        this.callLogService = callLogService;
    }

    @PostMapping
    public ResponseEntity<CallLogResponse> create(
            @CurrentUserId Long userId,
            @RequestBody CreateCallRequest request) {
        CallLog log = new CallLog();
        log.setPhoneNumber(request.phoneNumber());
        log.setCallType(request.callType());
        log.setStatus(request.status());
        log.setDurationSeconds(request.durationSeconds());
        log.setNotes(request.notes());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                toResponse(callLogService.createCallLog(userId, log)));
    }

    @GetMapping
    public List<CallLogResponse> getAll(@CurrentUserId Long userId) {
        return callLogService.getCallLogs(userId)
                .stream().map(CallLogController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CallLogResponse getOne(
            @CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(callLogService.getCallLog(userId, id));
    }

    @PatchMapping("/{id}/status")
    public CallLogResponse updateStatus(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @RequestBody UpdateCallStatusRequest request) {
        return toResponse(callLogService.updateCallStatus(
                userId, id, request.status(), request.durationSeconds()));
    }

    @GetMapping("/status/{status}")
    public List<CallLogResponse> getByStatus(
            @CurrentUserId Long userId,
            @PathVariable CallLog.CallStatus status) {
        return callLogService.getCallLogsByStatus(userId, status)
                .stream().map(CallLogController::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId Long userId, @PathVariable Long id) {
        callLogService.deleteCallLog(userId, id);
        return ResponseEntity.noContent().build();
    }

    private static CallLogResponse toResponse(CallLog c) {
        return new CallLogResponse(
                c.getId(), c.getPhoneNumber(), c.getCallType(),
                c.getStatus(), c.getDurationSeconds(),
                c.getNotes(), c.getCreatedAt());
    }

    public record CreateCallRequest(
            String phoneNumber,
            CallLog.CallType callType,
            CallLog.CallStatus status,
            Integer durationSeconds,
            String notes) {}

    public record UpdateCallStatusRequest(
            CallLog.CallStatus status, Integer durationSeconds) {}

    public record CallLogResponse(
            Long id, String phoneNumber,
            CallLog.CallType callType,
            CallLog.CallStatus status,
            Integer durationSeconds, String notes,
            LocalDateTime createdAt) {}
}