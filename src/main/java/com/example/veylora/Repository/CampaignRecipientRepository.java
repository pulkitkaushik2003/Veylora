package com.example.veylora.Repository;

import com.example.veylora.Model.CampaignRecipient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, Long> {

    List<CampaignRecipient> findByCampaign_IdOrderByIdAsc(Long campaignId);

    List<CampaignRecipient> findByCampaign_IdAndStatus(Long campaignId, CampaignRecipient.RecipientStatus status);

    long countByCampaign_Id(Long campaignId);

    long countByCampaign_IdAndStatus(Long campaignId, CampaignRecipient.RecipientStatus status);

    long countByCampaign_IdAndStatusIn(Long campaignId, Collection<CampaignRecipient.RecipientStatus> statuses);

    long countByCampaign_IdAndLastDispatchedAtAfter(Long campaignId, LocalDateTime after);

    @Query("select r.phoneNumber from CampaignRecipient r where r.campaign.id = :campaignId")
    List<String> findPhones(@Param("campaignId") Long campaignId);

    /** Recipients that are ready to be sent now, oldest first. */
    @Query("select r from CampaignRecipient r where r.campaign.id = :campaignId and r.status = :status "
            + "and (r.nextAttemptAt is null or r.nextAttemptAt <= :now) order by r.id")
    List<CampaignRecipient> findDue(@Param("campaignId") Long campaignId,
                                    @Param("status") CampaignRecipient.RecipientStatus status,
                                    @Param("now") LocalDateTime now,
                                    Pageable pageable);

    /** Exotel attempts that were sent and have no final result yet. */
    @Query("select r from CampaignRecipient r join fetch r.campaign c join fetch c.user "
            + "where r.status = :status and r.providerSid is not null order by r.id")
    List<CampaignRecipient> findAwaitingProvider(@Param("status") CampaignRecipient.RecipientStatus status);
}
