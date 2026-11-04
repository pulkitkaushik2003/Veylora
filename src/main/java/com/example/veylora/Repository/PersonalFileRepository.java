package com.example.veylora.Repository;

import com.example.veylora.Model.PersonalFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonalFileRepository extends JpaRepository<PersonalFile, Long> {

    /** Light view (no content). */
    interface Info {
        Long getId();
        String getFileName();
        String getMatchKey();
        long getSizeBytes();
    }

    @Query("select f.id as id, f.fileName as fileName, f.matchKey as matchKey, f.sizeBytes as sizeBytes "
            + "from PersonalFile f where f.campaign.id = :campaignId order by f.id")
    List<Info> listInfo(@Param("campaignId") Long campaignId);

    Optional<PersonalFile> findByIdAndCampaign_Id(Long id, Long campaignId);

    @Query("select f.id from PersonalFile f where f.campaign.id = :campaignId and f.matchKey = :matchKey")
    List<Long> idsByKey(@Param("campaignId") Long campaignId, @Param("matchKey") String matchKey);

    @Query("select coalesce(sum(f.sizeBytes), 0) from PersonalFile f where f.campaign.id = :campaignId")
    long totalSize(@Param("campaignId") Long campaignId);

    long countByCampaign_Id(Long campaignId);

    @Modifying
    @Query("delete from PersonalFile f where f.campaign.id = :campaignId")
    void deleteAllFor(@Param("campaignId") Long campaignId);
}
