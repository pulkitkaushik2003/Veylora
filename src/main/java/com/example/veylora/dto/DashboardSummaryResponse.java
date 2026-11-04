package com.example.veylora.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
        long totalContacts,
        long callsToday,
        long answeredCalls,
        long missedCalls,
        long failedCalls,
        long smsSubmitted,
        long smsDelivered,
        long smsFailed,
        long emailsAccepted,
        long emailsBounced,
        long emailsFailed,
        long activeCampaigns,
        long onlineDevices,
        long totalDevices,
        LocalDateTime generatedAt,
        List<DashboardActivityItem> recentActivity,
        List<DashboardChannelStat> channelStats
) {}
