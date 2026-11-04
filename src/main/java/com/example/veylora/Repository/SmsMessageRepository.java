package com.example.veylora.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.SmsMessage;

import java.util.List;
import java.util.Optional;

public interface SmsMessageRepository
        extends JpaRepository<SmsMessage, Long> {

    List<SmsMessage> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<SmsMessage> findByIdAndUser_Id(Long id, Long userId);

    List<SmsMessage> findByUser_IdAndStatus(
        Long userId, SmsMessage.SmsStatus status
    );

    List<SmsMessage> findByUser_IdAndRecipientPhoneContaining(
        Long userId, String phone
    );
}
 