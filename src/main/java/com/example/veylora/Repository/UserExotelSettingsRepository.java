package com.example.veylora.Repository;

import com.example.veylora.Model.UserExotelSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserExotelSettingsRepository extends JpaRepository<UserExotelSettings, Long> {
    Optional<UserExotelSettings> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
