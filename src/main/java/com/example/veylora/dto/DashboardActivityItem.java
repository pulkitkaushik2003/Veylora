package com.example.veylora.dto;

import java.time.LocalDateTime;

public record DashboardActivityItem(
        LocalDateTime timestamp,
        String type,
        String title,
        String detail
) {}
