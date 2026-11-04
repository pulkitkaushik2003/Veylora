package com.example.veylora.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.CallLog;

import java.util.List;
import java.util.Optional;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    List<CallLog> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<CallLog> findByIdAndUser_Id(Long id, Long userId);

    List<CallLog> findByUser_IdAndStatus(
        Long userId, CallLog.CallStatus status
    );

    List<CallLog> findByUser_IdAndPhoneNumberContaining(
        Long userId, String phoneNumber
    );
}
