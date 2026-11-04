package com.example.veylora.Repository;

import com.example.veylora.Model.AudioAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AudioAssetRepository extends JpaRepository<AudioAsset, Long> {

    List<AudioAsset> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<AudioAsset> findByIdAndUser_Id(Long id, Long userId);
}
