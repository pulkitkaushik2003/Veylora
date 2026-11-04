package com.example.veylora.dto;


import com.example.veylora.Model.Campaign;
import java.time.LocalDateTime;

public record CampaignResponse(
        Long id,
        String name,
        String description,
        Campaign.CampaignType type,
        Campaign.CampaignStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        int totalRecipients,
        int successfulCount,
        int failedCount,
        Integer rateLimitPerMinute,
        Integer maxRetries,
        Integer retryDelayMinutes,
        Boolean retryOnNoAnswer,
        String selectedTemplateId,
        String selectedAudioId,
        String selectedAiAgentId,
        String selectedDeviceId,
        Integer selectedSimSlot,
        String createdBy,
        LocalDateTime createdAt
) {}