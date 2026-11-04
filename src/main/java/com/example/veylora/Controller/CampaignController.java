package com.example.veylora.Controller;

import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.CampaignRecipient;
import com.example.veylora.Service.CampaignRunService;
import com.example.veylora.Service.CampaignService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Campaigns of the logged-in user. The user always comes from the session, never from a parameter. */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignRunService runService;

    public CampaignController(CampaignService campaignService, CampaignRunService runService) {
        this.campaignService = campaignService;
        this.runService = runService;
    }

    @GetMapping
    public List<CampaignResponse> getAll(@CurrentUserId Long userId) {
        return campaignService.getCampaigns(userId).stream().map(CampaignController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CampaignResponse getOne(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(campaignService.getCampaign(userId, id));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(@CurrentUserId Long userId,
                                                   @RequestBody CreateCampaignRequest request) {
        Campaign campaign = campaignService.createCampaign(userId, request.name(), request.description(),
                request.type(), request.scheduledAt(), request.status());
        campaign = campaignService.updateCampaignConfiguration(
                userId, campaign.getId(), request.rateLimitPerMinute(),
                request.maxRetries(), request.retryDelayMinutes(),
                request.retryOnNoAnswer(), request.selectedTemplateId(),
                request.selectedAudioId(), request.selectedAiAgentId(),
                request.selectedDeviceId(), request.selectedSimSlot());
        if (request.messageBody() != null) {
            campaign = runService.setMessageBody(userId, campaign.getId(), request.messageBody());
        }
        if (request.emailSubject() != null) {
            campaign = runService.setEmailSubject(userId, campaign.getId(), request.emailSubject());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(campaign));
    }

    /** Kept for older callers. RUNNING / PAUSED / COMPLETED go through the same logic as the buttons. */
    @PatchMapping("/{id}/status")
    public CampaignResponse updateStatus(@CurrentUserId Long userId, @PathVariable Long id,
                                         @RequestBody UpdateCampaignStatusRequest request) {
        Campaign.CampaignStatus status = request.status();
        if (status == null) throw new IllegalArgumentException("Campaign status is required");
        Campaign current = campaignService.getCampaign(userId, id);
        return toResponse(switch (status) {
            case RUNNING -> current.getStatus() == Campaign.CampaignStatus.PAUSED
                    ? runService.resume(userId, id) : runService.start(userId, id);
            case PAUSED -> runService.pause(userId, id);
            case COMPLETED -> runService.cancel(userId, id);
            default -> campaignService.updateCampaignStatus(userId, id, status);
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        campaignService.deleteCampaign(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- recipients

    @PostMapping("/{id}/recipients")
    public CampaignRunService.AddResult addRecipients(@CurrentUserId Long userId, @PathVariable Long id,
                                   @RequestBody AddRecipientsRequest request) {
        List<CampaignRunService.RecipientInput> inputs = request.recipients() == null ? List.of()
                : request.recipients().stream()
                .map(r -> new CampaignRunService.RecipientInput(r.name(), r.phoneNumber(), r.email())).toList();
        return runService.addRecipients(userId, id, inputs, request.contactIds());
    }

    @GetMapping("/{id}/recipients")
    public List<RecipientResponse> recipients(@CurrentUserId Long userId, @PathVariable Long id) {
        return runService.listRecipients(userId, id).stream().map(r -> new RecipientResponse(
                r.getId(), r.getName(), r.getPhoneNumber(), r.getEmail(), r.getStatus(),
                r.getAttempts(), r.getLastError())).toList();
    }

    // ------------------------------------------------------------------- actions

    @PostMapping("/{id}/start")
    public CampaignResponse start(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(runService.start(userId, id));
    }

    @PostMapping("/{id}/pause")
    public CampaignResponse pause(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(runService.pause(userId, id));
    }

    @PostMapping("/{id}/resume")
    public CampaignResponse resume(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(runService.resume(userId, id));
    }

    @PostMapping("/{id}/cancel")
    public CampaignResponse cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(runService.cancel(userId, id));
    }

    @PostMapping("/{id}/retry-failed")
    public CampaignResponse retryFailed(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(runService.retryFailed(userId, id));
    }

    // -------------------------------------------------------------------- shapes

    private static CampaignResponse toResponse(Campaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getDescription(), c.getType(), c.getStatus(),
                c.getScheduledAt(), c.getStartedAt(), c.getCompletedAt(),
                c.getTotalRecipients(), c.getSuccessfulCount(), c.getFailedCount(),
                c.getRateLimitPerMinute(), c.getMaxRetries(), c.getRetryDelayMinutes(), c.getRetryOnNoAnswer(),
                c.getSelectedTemplateId(), c.getSelectedAudioId(), c.getSelectedAiAgentId(),
                c.getSelectedDeviceId(), c.getSelectedSimSlot(), c.getMessageBody(), c.getEmailSubject(),
                c.getCreatedBy(), c.getCreatedAt());
    }

    public record CampaignResponse(
            Long id, String name, String description,
            Campaign.CampaignType type, Campaign.CampaignStatus status,
            LocalDateTime scheduledAt, LocalDateTime startedAt, LocalDateTime completedAt,
            Integer totalRecipients, Integer successfulCount, Integer failedCount,
            Integer rateLimitPerMinute, Integer maxRetries, Integer retryDelayMinutes,
            Boolean retryOnNoAnswer, String selectedTemplateId, String selectedAudioId,
            String selectedAiAgentId, String selectedDeviceId, Integer selectedSimSlot,
            String messageBody, String emailSubject, String createdBy, LocalDateTime createdAt) {}

    public record CreateCampaignRequest(
            String name, String description, Campaign.CampaignType type,
            LocalDateTime scheduledAt, Campaign.CampaignStatus status,
            Integer totalRecipients, Integer rateLimitPerMinute, Integer maxRetries,
            Integer retryDelayMinutes, Boolean retryOnNoAnswer,
            String selectedTemplateId, String selectedAudioId, String selectedAiAgentId,
            String selectedDeviceId, Integer selectedSimSlot, String messageBody, String emailSubject) {}

    public record UpdateCampaignStatusRequest(Campaign.CampaignStatus status) {}

    public record RecipientBody(String name, String phoneNumber, String email) {}

    public record AddRecipientsRequest(List<RecipientBody> recipients, List<Long> contactIds) {}

    public record RecipientResponse(Long id, String name, String phoneNumber, String email,
                                    CampaignRecipient.RecipientStatus status,
                                    Integer attempts, String lastError) {}
}
