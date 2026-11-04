package com.example.veylora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank (message = "Device UID is required")
        @Size (max = 150)
        String deviceUid,

        @NotBlank(message = "Device name is required")
        @Size(max = 100)
        String name,

        @Size(max = 100)
        String model,

        @Size(max = 100)
        String manufacturer,

        @Size(max = 50)
        String platform
) {}
