package com.example.veylora.dto;

import com.example.veylora.Model.SmsMessage;
import java.time.LocalDateTime;

public record SmsMessageResponse(
        Long id,
        String recipientPhone,
        String messageBody,
        SmsMessage.SmsStatus status,
        String providerMessageId,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {}
