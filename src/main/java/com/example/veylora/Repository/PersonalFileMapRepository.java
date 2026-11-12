package com.example.veylora.Repository;

import com.example.veylora.Model.PersonalFileMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonalFileMapRepository extends JpaRepository<PersonalFileMap, Long> {

    List<PersonalFileMap> findByCampaignId(Long campaignId);

    long countByCampaignId(Long campaignId);

    @Modifying
    @Query("delete from PersonalFileMap m where m.campaignId = :campaignId")
    void deleteAllFor(@Param("campaignId") Long campaignId);
}
