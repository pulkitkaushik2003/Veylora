package com.example.veylora.Repository;

import com.example.veylora.Model.CampaignAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignAttachmentRepository extends JpaRepository<CampaignAttachment, Long> {

    /** Light view of a file (no content), for lists. */
    interface Info {
        Long getId();
        String getFileName();
        String getContentType();
        long getSizeBytes();
    }

    @Query("select a.id as id, a.fileName as fileName, a.contentType as contentType, a.sizeBytes as sizeBytes "
            + "from CampaignAttachment a where a.campaign.id = :campaignId order by a.id")
    List<Info> listInfo(@Param("campaignId") Long campaignId);

    /** Full files with content, used when sending. */
    List<CampaignAttachment> findByCampaign_IdOrderByIdAsc(Long campaignId);

    Optional<CampaignAttachment> findByIdAndCampaign_Id(Long id, Long campaignId);

    @Query("select coalesce(sum(a.sizeBytes), 0) from CampaignAttachment a where a.campaign.id = :campaignId")
    long totalSize(@Param("campaignId") Long campaignId);

    long countByCampaign_Id(Long campaignId);
}
