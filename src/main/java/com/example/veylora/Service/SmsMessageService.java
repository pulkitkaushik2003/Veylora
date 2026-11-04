package com.example.veylora.Service;

import com.example.veylora.Model.SmsMessage;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.SmsMessageRepository;
import com.example.veylora.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SmsMessageService {

    private final SmsMessageRepository smsRepository;
    private final UserRepository userRepository;

    public SmsMessageService(SmsMessageRepository smsRepository,
                             UserRepository userRepository) {
        this.smsRepository = smsRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SmsMessage createSms(Long userId, String recipientPhone,
                               String messageBody, String dltTemplateId,
                               String dltHeader) {
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new IllegalArgumentException("Recipient phone is required");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("Message body is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SmsMessage sms = new SmsMessage();
        sms.setRecipientPhone(recipientPhone);
        sms.setMessageBody(messageBody);
        sms.setDltTemplateId(dltTemplateId);
        sms.setDltHeader(dltHeader);
        sms.setStatus(SmsMessage.SmsStatus.QUEUED);
        sms.setUser(user);

        // QUEUED means saved for sending; it has not been sent yet.
        return smsRepository.save(sms);
    }

    @Transactional(readOnly = true)
    public List<SmsMessage> getMessages(Long userId) {
        return smsRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public SmsMessage getMessage(Long userId, Long smsId) {
        return smsRepository.findByIdAndUser_Id(smsId, userId)
                .orElseThrow(() -> new IllegalArgumentException("SMS not found"));
    }

    @Transactional(readOnly = true)
    public List<SmsMessage> getMessagesByStatus(
            Long userId, SmsMessage.SmsStatus status) {
        return smsRepository.findByUser_IdAndStatus(userId, status);
    }

    @Transactional
    public SmsMessage updateSmsStatus(Long userId, Long smsId,
                                      SmsMessage.SmsStatus status,
                                      String providerMessageId,
                                      String errorMessage) {
        if (status == null) {
            throw new IllegalArgumentException("SMS status is required");
        }

        SmsMessage sms = getMessage(userId, smsId);
        sms.setStatus(status);
        sms.setProviderMessageId(providerMessageId);
        sms.setErrorMessage(errorMessage);

        if (status == SmsMessage.SmsStatus.SENT
                || status == SmsMessage.SmsStatus.DELIVERED) {
            if (sms.getSentAt() == null) {
                sms.setSentAt(LocalDateTime.now());
            }
        }

        return smsRepository.save(sms);
    }

    @Transactional
    public void deleteMessage(Long userId, Long smsId) {
        smsRepository.delete(getMessage(userId, smsId));
    }
}