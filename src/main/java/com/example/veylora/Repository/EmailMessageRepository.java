package com.example.veylora.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.EmailMessage;

import java.util.List;
import java.util.Optional;

public interface EmailMessageRepository
        extends JpaRepository<EmailMessage, Long> {

    List<EmailMessage> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<EmailMessage> findByIdAndUser_Id(Long id, Long userId);

    List<EmailMessage> findByUser_IdAndStatus(
        Long userId, EmailMessage.EmailStatus status
    );

    List<EmailMessage> findByUser_IdAndRecipientEmailContainingIgnoreCase(
        Long userId, String email
    );
}

