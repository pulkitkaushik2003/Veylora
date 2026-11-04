package com.example.veylora.dto;


import com.example.veylora.Model.EmailMessage;
import java.time.LocalDateTime;

public record EmailMessageResponse(
        Long id,
        String recipientEmail,
        String subject,
        String body,
        EmailMessage.EmailStatus status,
        String providerMessageId,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {}
