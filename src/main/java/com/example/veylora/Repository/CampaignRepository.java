package com.example.veylora.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.Campaign;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository
        extends JpaRepository<Campaign, Long> {

    List<Campaign> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Campaign> findByIdAndUser_Id(Long id, Long userId);

    List<Campaign> findByUser_IdAndStatus(
        Long userId, Campaign.CampaignStatus status
    );

    List<Campaign> findByUser_IdAndType(
        Long userId, Campaign.CampaignType type
    );

    List<Campaign> findByStatusIn(Collection<Campaign.CampaignStatus> statuses);
}
