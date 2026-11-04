package com.example.veylora.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.SimCard;

import java.util.List;
import java.util.Optional;

public interface SimCardRepository extends JpaRepository<SimCard, Long> {

    List<SimCard> findByDevice_Id(Long deviceId);

    Optional<SimCard> findByIdAndDevice_Id(Long id, Long deviceId);

    List<SimCard> findByDevice_User_Id(Long userId);

    List<SimCard> findByDevice_IdAndStatus(
        Long deviceId, SimCard.SimStatus status
    );
}
