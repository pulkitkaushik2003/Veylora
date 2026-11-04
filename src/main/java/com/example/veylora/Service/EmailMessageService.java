package com.example.veylora.Service;


import com.example.veylora.Model.EmailMessage;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.EmailMessageRepository;
import com.example.veylora.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailMessageService {

    private final EmailMessageRepository emailRepository;
    private final UserRepository userRepository;

    public EmailMessageService(EmailMessageRepository emailRepository,
                               UserRepository userRepository) {
        this.emailRepository = emailRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EmailMessage createEmail(Long userId, String recipientEmail,
                                   String subject, String body) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Email body is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        EmailMessage email = new EmailMessage();
        email.setRecipientEmail(recipientEmail);
        email.setSubject(subject);
        email.setBody(body);
        email.setStatus(EmailMessage.EmailStatus.QUEUED);
        email.setUser(user);

        return emailRepository.save(email);
    }

    @Transactional(readOnly = true)
    public List<EmailMessage> getEmails(Long userId) {
        return emailRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public EmailMessage getEmail(Long userId, Long emailId) {
        return emailRepository.findByIdAndUser_Id(emailId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
    }

    @Transactional(readOnly = true)
    public List<EmailMessage> getEmailsByStatus(
            Long userId, EmailMessage.EmailStatus status) {
        return emailRepository.findByUser_IdAndStatus(userId, status);
    }

    @Transactional
    public EmailMessage updateEmailStatus(Long userId, Long emailId,
                                          EmailMessage.EmailStatus status,
                                          String providerMessageId,
                                          String errorMessage) {
        if (status == null) {
            throw new IllegalArgumentException("Email status is required");
        }

        EmailMessage email = getEmail(userId, emailId);
        email.setStatus(status);
        email.setProviderMessageId(providerMessageId);
        email.setErrorMessage(errorMessage);

        if (status == EmailMessage.EmailStatus.SENT
                || status == EmailMessage.EmailStatus.DELIVERED) {
            if (email.getSentAt() == null) {
                email.setSentAt(LocalDateTime.now());
            }
        }

        return emailRepository.save(email);
    }

    @Transactional
    public void deleteEmail(Long userId, Long emailId) {
        emailRepository.delete(getEmail(userId, emailId));
    }
}