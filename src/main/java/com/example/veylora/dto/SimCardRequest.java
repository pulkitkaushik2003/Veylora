package com.example.veylora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
public record SimCardRequest(
        @Size (max = 100)
        String label,

        @NotBlank (message = "Phone number is required")
        @Size(max = 30)
        String phoneNumber,

        @Size(max = 100)
        String carrier,

        @PositiveOrZero (message = "Slot index cannot be negative")
        Integer slotIndex
) {}
