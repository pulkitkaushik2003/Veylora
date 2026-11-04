package com.example.veylora.dto;


import com.example.veylora.Model.Device;
import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String deviceUid,
        String name,
        String model,
        String manufacturer,
        String platform,
        Device.DeviceStatus status,
        LocalDateTime lastSeenAt,
        LocalDateTime createdAt
) {}
