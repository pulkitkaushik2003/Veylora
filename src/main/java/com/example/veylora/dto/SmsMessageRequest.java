package com.example.veylora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SmsMessageRequest(
        @NotBlank (message = "Recipient phone is required")
        @Size (max = 30)
        String recipientPhone,

        @NotBlank(message = "Message body is required")
        @Size(max = 5000)
        String messageBody
) {}