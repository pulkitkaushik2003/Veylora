package com.example.veylora.dto;


import java.time.LocalDateTime;

import com.example.veylora.Model.SimCard;

public record SimCardResponse(
        Long id,
        String label,
        String phoneNumber,
        String carrier,
        Integer slotIndex,
        SimCard.SimStatus status,
        LocalDateTime createdAt
) {}