package com.example.veylora.Controller;


import com.example.veylora.config.CurrentUserId;
import com.example.veylora.Model.SmsMessage;
import com.example.veylora.Service.SmsMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sms")
public class SmsMessageController {

    private final SmsMessageService smsService;

    public SmsMessageController(SmsMessageService smsService) {
        this.smsService = smsService;
    }

    @PostMapping
    public ResponseEntity<SmsResponse> create(
            @CurrentUserId Long userId,
            @RequestBody CreateSmsRequest request) {
        SmsMessage sms = smsService.createSms(
                userId, request.recipientPhone(), request.messageBody(),
                request.dltTemplateId(), request.dltHeader());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(sms));
    }

    @GetMapping
    public List<SmsResponse> getAll(@CurrentUserId Long userId) {
        return smsService.getMessages(userId)
                .stream().map(SmsMessageController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SmsResponse getOne(
            @CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(smsService.getMessage(userId, id));
    }

    @GetMapping("/status/{status}")
    public List<SmsResponse> getByStatus(
            @CurrentUserId Long userId,
            @PathVariable SmsMessage.SmsStatus status) {
        return smsService.getMessagesByStatus(userId, status)
                .stream().map(SmsMessageController::toResponse).toList();
    }

    @PatchMapping("/{id}/status")
    public SmsResponse updateStatus(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @RequestBody UpdateSmsStatusRequest request) {
        return toResponse(smsService.updateSmsStatus(
                userId, id, request.status(),
                request.providerMessageId(), request.errorMessage()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId Long userId, @PathVariable Long id) {
        smsService.deleteMessage(userId, id);
        return ResponseEntity.noContent().build();
    }

    private static SmsResponse toResponse(SmsMessage s) {
        return new SmsResponse(
                s.getId(), s.getRecipientPhone(), s.getMessageBody(),
                s.getDltTemplateId(), s.getDltHeader(),
                s.getStatus(), s.getProviderMessageId(),
                s.getErrorMessage(), s.getCreatedAt(), s.getSentAt());
    }

    public record CreateSmsRequest(
            String recipientPhone, String messageBody,
            String dltTemplateId, String dltHeader) {}

    public record UpdateSmsStatusRequest(
            SmsMessage.SmsStatus status,
            String providerMessageId,
            String errorMessage) {}

    public record SmsResponse(
            Long id, String recipientPhone, String messageBody,
            String dltTemplateId, String dltHeader,
            SmsMessage.SmsStatus status, String providerMessageId,
            String errorMessage, LocalDateTime createdAt,
            LocalDateTime sentAt) {}
}
