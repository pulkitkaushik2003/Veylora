package com.example.veylora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.example.veylora.Model.CallLog;

public record CallLogRequest(
        @NotBlank (message = "Phone number is required")
        @Size (max = 30)
        String phoneNumber,

        @NotNull (message = "Call type is required")
        CallLog.CallType callType,

        CallLog.CallStatus status,

        @PositiveOrZero (message = "Duration cannot be negative")
        Integer durationSeconds,

        @Size(max = 1000)
        String notes
) {}
