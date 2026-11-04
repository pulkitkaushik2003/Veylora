package com.example.veylora.Service;

import com.example.veylora.Model.CallLog;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.CallLogRepository;
import com.example.veylora.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CallLogService {

    private final CallLogRepository callLogRepository;
    private final UserRepository userRepository;

    public CallLogService(CallLogRepository callLogRepository,
                          UserRepository userRepository) {
        this.callLogRepository = callLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CallLog createCallLog(Long userId, CallLog request) {
        if (request.getPhoneNumber() == null
                || request.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CallLog log = new CallLog();
        log.setPhoneNumber(request.getPhoneNumber());
        log.setCallType(request.getCallType());
        log.setStatus(request.getStatus() != null
                ? request.getStatus() : CallLog.CallStatus.INITIATED);
        log.setDurationSeconds(request.getDurationSeconds());
        log.setNotes(request.getNotes());
        log.setUser(user);

        return callLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<CallLog> getCallLogs(Long userId) {
        return callLogRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public CallLog getCallLog(Long userId, Long callLogId) {
        return callLogRepository.findByIdAndUser_Id(callLogId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Call log not found"));
    }

    @Transactional
    public CallLog updateCallStatus(Long userId, Long callLogId,
                                    CallLog.CallStatus status,
                                    Integer durationSeconds) {
        if (status == null) {
            throw new IllegalArgumentException("Call status is required");
        }

        CallLog log = getCallLog(userId, callLogId);
        log.setStatus(status);

        if (durationSeconds != null) {
            if (durationSeconds < 0) {
                throw new IllegalArgumentException("Duration cannot be negative");
            }
            log.setDurationSeconds(durationSeconds);
        }

        return callLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<CallLog> getCallLogsByStatus(Long userId,
                                             CallLog.CallStatus status) {
        return callLogRepository.findByUser_IdAndStatus(userId, status);
    }

    @Transactional
    public void deleteCallLog(Long userId, Long callLogId) {
        callLogRepository.delete(getCallLog(userId, callLogId));
    }
}