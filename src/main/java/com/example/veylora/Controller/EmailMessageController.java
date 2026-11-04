package com.example.veylora.Controller;



import com.example.veylora.Model.EmailMessage;
import com.example.veylora.Service.EmailMessageService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailMessageController {

    private final EmailMessageService emailService;

    public EmailMessageController(EmailMessageService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<EmailResponse> create(
            @CurrentUserId Long userId,
            @RequestBody CreateEmailRequest request) {
        EmailMessage email = emailService.createEmail(
                userId, request.recipientEmail(),
                request.subject(), request.body());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(email));
    }

    @GetMapping
    public List<EmailResponse> getAll(@CurrentUserId Long userId) {
        return emailService.getEmails(userId)
                .stream().map(EmailMessageController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EmailResponse getOne(
            @CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(emailService.getEmail(userId, id));
    }

    @GetMapping("/status/{status}")
    public List<EmailResponse> getByStatus(
            @CurrentUserId Long userId,
            @PathVariable EmailMessage.EmailStatus status) {
        return emailService.getEmailsByStatus(userId, status)
                .stream().map(EmailMessageController::toResponse).toList();
    }

    @PatchMapping("/{id}/status")
    public EmailResponse updateStatus(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @RequestBody UpdateEmailStatusRequest request) {
        return toResponse(emailService.updateEmailStatus(
                userId, id, request.status(),
                request.providerMessageId(), request.errorMessage()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId Long userId, @PathVariable Long id) {
        emailService.deleteEmail(userId, id);
        return ResponseEntity.noContent().build();
    }

    private static EmailResponse toResponse(EmailMessage e) {
        return new EmailResponse(
                e.getId(), e.getRecipientEmail(), e.getSubject(),
                e.getBody(), e.getStatus(), e.getProviderMessageId(),
                e.getErrorMessage(), e.getCreatedAt(), e.getSentAt());
    }

    public record CreateEmailRequest(
            String recipientEmail, String subject, String body) {}

    public record UpdateEmailStatusRequest(
            EmailMessage.EmailStatus status,
            String providerMessageId,
            String errorMessage) {}

    public record EmailResponse(
            Long id, String recipientEmail, String subject, String body,
            EmailMessage.EmailStatus status, String providerMessageId,
            String errorMessage, LocalDateTime createdAt,
            LocalDateTime sentAt) {}
}