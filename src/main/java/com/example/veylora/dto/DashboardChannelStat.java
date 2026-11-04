package com.example.veylora.dto;

public record DashboardChannelStat(
        String channel,
        long submitted,
        long delivered,
        long failed,
        double successRate
) {}
