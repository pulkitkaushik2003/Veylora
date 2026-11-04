package com.example.veylora.Service;


import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.CampaignRepository;
import com.example.veylora.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           UserRepository userRepository) {
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Campaign createCampaign(Long userId, String name,
                                   String description,
                                   Campaign.CampaignType type,
                                   LocalDateTime scheduledAt) {
        return createCampaign(userId, name, description, type, scheduledAt, null);
    }

    @Transactional
    public Campaign createCampaign(Long userId, String name,
                                   String description,
                                   Campaign.CampaignType type,
                                   LocalDateTime scheduledAt,
                                   Campaign.CampaignStatus requestedStatus) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Campaign type is required");
        }
        if (scheduledAt != null && scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Schedule time cannot be in the past");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Campaign campaign = new Campaign();
        campaign.setName(name);
        campaign.setDescription(description);
        campaign.setType(type);
        campaign.setStatus(requestedStatus != null
                ? requestedStatus
                : scheduledAt == null
                    ? Campaign.CampaignStatus.DRAFT
                    : Campaign.CampaignStatus.SCHEDULED);
        campaign.setScheduledAt(scheduledAt);
        campaign.setTotalRecipients(0);
        campaign.setSuccessfulCount(0);
        campaign.setFailedCount(0);
        campaign.setUser(user);
        campaign.setCreatedBy(user.getName());

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign updateCampaignConfiguration(
            Long userId, Long campaignId, Integer rateLimitPerMinute,
            Integer maxRetries, Integer retryDelayMinutes,
            Boolean retryOnNoAnswer, String selectedTemplateId,
            String selectedAudioId, String selectedAiAgentId,
            String selectedDeviceId, Integer selectedSimSlot) {
        if (rateLimitPerMinute != null && rateLimitPerMinute < 1) {
            throw new IllegalArgumentException("Rate limit must be positive");
        }
        if (maxRetries != null && maxRetries < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
        if (retryDelayMinutes != null && retryDelayMinutes < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
        if (selectedSimSlot != null && selectedSimSlot != 1 && selectedSimSlot != 2) {
            throw new IllegalArgumentException("SIM slot must be 1 or 2");
        }

        Campaign campaign = getCampaign(userId, campaignId);
        campaign.setRateLimitPerMinute(rateLimitPerMinute);
        campaign.setMaxRetries(maxRetries);
        campaign.setRetryDelayMinutes(retryDelayMinutes);
        campaign.setRetryOnNoAnswer(retryOnNoAnswer);
        campaign.setSelectedTemplateId(selectedTemplateId);
        campaign.setSelectedAudioId(selectedAudioId);
        campaign.setSelectedAiAgentId(selectedAiAgentId);
        campaign.setSelectedDeviceId(selectedDeviceId);
        campaign.setSelectedSimSlot(selectedSimSlot);
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<Campaign> getCampaigns(Long userId) {
        return campaignRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaign(Long userId, Long campaignId) {
        return campaignRepository.findByIdAndUser_Id(campaignId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    }

    @Transactional(readOnly = true)
    public List<Campaign> getCampaignsByStatus(
            Long userId, Campaign.CampaignStatus status) {
        return campaignRepository.findByUser_IdAndStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public List<Campaign> getCampaignsByType(
            Long userId, Campaign.CampaignType type) {
        return campaignRepository.findByUser_IdAndType(userId, type);
    }

    @Transactional
    public Campaign updateCampaignStatus(
            Long userId, Long campaignId,
            Campaign.CampaignStatus newStatus) {

        if (newStatus == null) {
            throw new IllegalArgumentException("Campaign status is required");
        }

        Campaign campaign = getCampaign(userId, campaignId);

        if (campaign.getStatus() == Campaign.CampaignStatus.COMPLETED
                || campaign.getStatus() == Campaign.CampaignStatus.FAILED) {
            throw new IllegalStateException(
                    "A completed or failed campaign cannot be restarted using this operation");
        }

        campaign.setStatus(newStatus);

        if (newStatus == Campaign.CampaignStatus.RUNNING
                && campaign.getStartedAt() == null) {
            campaign.setStartedAt(LocalDateTime.now());
        }

        if (newStatus == Campaign.CampaignStatus.COMPLETED
                || newStatus == Campaign.CampaignStatus.FAILED) {
            campaign.setCompletedAt(LocalDateTime.now());
        }

        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign updateCampaignCounts(
            Long userId, Long campaignId,
            int totalRecipients, int successfulCount, int failedCount) {

        if (totalRecipients < 0 || successfulCount < 0 || failedCount < 0) {
            throw new IllegalArgumentException("Campaign counts cannot be negative");
        }

        if (successfulCount + failedCount > totalRecipients) {
            throw new IllegalArgumentException(
                    "Successful and failed counts cannot exceed total recipients");
        }

        Campaign campaign = getCampaign(userId, campaignId);
        campaign.setTotalRecipients(totalRecipients);
        campaign.setSuccessfulCount(successfulCount);
        campaign.setFailedCount(failedCount);

        return campaignRepository.save(campaign);
    }

    @Transactional
    public void deleteCampaign(Long userId, Long campaignId) {
        Campaign campaign = getCampaign(userId, campaignId);

        if (campaign.getStatus() == Campaign.CampaignStatus.RUNNING) {
            throw new IllegalStateException("Stop the running campaign before deleting it");
        }

        campaignRepository.delete(campaign);
    }
}