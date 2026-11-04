package com.example.veylora.Service;

import com.example.veylora.Model.Campaign;
import com.example.veylora.Repository.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** Every few seconds, hands the next recipients of each running campaign to the phone. */
@Component
public class CampaignDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CampaignDispatcher.class);

    private final CampaignRepository campaignRepository;
    private final CampaignRunService runService;
    private final TransactionTemplate tx;

    public CampaignDispatcher(CampaignRepository campaignRepository,
                              CampaignRunService runService,
                              TransactionTemplate tx) {
        this.campaignRepository = campaignRepository;
        this.runService = runService;
        this.tx = tx;
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 15_000)
    public void tick() {
        List<Campaign> active = campaignRepository.findByStatusIn(
                List.of(Campaign.CampaignStatus.RUNNING, Campaign.CampaignStatus.SCHEDULED));
        for (Campaign campaign : active) {
            Long id = campaign.getId();
            try {
                // One transaction per campaign: a problem in one never affects the others.
                tx.executeWithoutResult(status -> runService.dispatchOne(id));
            } catch (Exception e) {
                log.error("Dispatch failed for campaign {}", id, e);
                try {
                    tx.executeWithoutResult(status -> runService.pauseAfterError(id, e.getMessage()));
                } catch (Exception ignored) {
                    // nothing more we can do; the next tick will try again
                }
            }
        }
    }
}
