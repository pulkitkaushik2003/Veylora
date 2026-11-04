package com.example.veylora.dto;



import java.time.LocalDateTime;

public record ContactResponse(
        Long id,
        String name,
        String phone,
        String email,
        String company,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
