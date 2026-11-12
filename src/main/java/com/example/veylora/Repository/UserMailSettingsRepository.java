package com.example.veylora.Repository;

import com.example.veylora.Model.UserMailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMailSettingsRepository extends JpaRepository<UserMailSettings, Long> {
    Optional<UserMailSettings> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
