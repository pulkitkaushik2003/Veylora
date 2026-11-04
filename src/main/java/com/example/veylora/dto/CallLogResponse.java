package com.example.veylora.dto;

import com.example.veylora.Model.CallLog;
import java.time.LocalDateTime;

public record CallLogResponse(
        Long id,
        String phoneNumber,
        CallLog.CallType callType,
        CallLog.CallStatus status,
        Integer durationSeconds,
        String notes,
        LocalDateTime createdAt
) {}