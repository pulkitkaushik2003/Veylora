package com.example.veylora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailMessageRequest(
        @NotBlank (message = "Recipient email is required")
        @Email (message = "Enter a valid email address")
        @Size (max = 254)
        String recipientEmail,

        @NotBlank(message = "Subject is required")
        @Size(max = 250)
        String subject,

        @NotBlank(message = "Email body is required")
        @Size(max = 20000)
        String body
) {}