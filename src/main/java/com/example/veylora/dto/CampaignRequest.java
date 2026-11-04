package com.example.veylora.dto;


import java.time.LocalDateTime;



import com.example.veylora.Model.Campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
        @NotBlank (message = "Campaign name is required")
        @Size (max = 150)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull (message = "Campaign type is required")
        Campaign.CampaignType type,

        LocalDateTime scheduledAt
) {}