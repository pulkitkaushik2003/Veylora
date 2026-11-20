package com.example.veylora.Service;

import com.example.veylora.Model.CallLog;
import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.CampaignRecipient;
import com.example.veylora.Model.CampaignRecipient.RecipientStatus;
import com.example.veylora.Model.DeviceTask;
import com.example.veylora.Model.SmsMessage;
import com.example.veylora.Repository.CampaignRecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs SMS and call campaigns through the user's Exotel account instead of a paired phone.
 *
 * Dispatch sends from the backend, like email. Results are not pushed to us (no public URL needed): reconcile()
 * asks Exotel for the status of every attempt that is still open. A final result is reported with the same
 * TaskFinishedEvent a phone uses, so retries, counters and "retry when not answered" work unchanged.
 */
@Service
public class ExotelCampaignService {

    private static final Logger log = LoggerFactory.getLogger(ExotelCampaignService.class);

    /** Give up waiting for a final status. Not retried automatically: the SMS or call may have gone out. */
    private static final Duration SMS_STUCK_AFTER = Duration.ofHours(2);
    private static final Duration CALL_STUCK_AFTER = Duration.ofHours(1);
    private static final int MAX_CHECKS_PER_TICK = 200;

    private final ExotelClient client;
    private final UserExotelSettingsService settings;
    private final CampaignRecipientRepository recipientRepository;
    private final SmsMessageService smsService;
    private final CallLogService callLogService;
    private final ApplicationEventPublisher events;

    public ExotelCampaignService(ExotelClient client, UserExotelSettingsService settings,
                                 CampaignRecipientRepository recipientRepository,
                                 SmsMessageService smsService, CallLogService callLogService,
                                 ApplicationEventPublisher events) {
        this.client = client;
        this.settings = settings;
        this.recipientRepository = recipientRepository;
        this.smsService = smsService;
        this.callLogService = callLogService;
        this.events = events;
    }

    /** Throws a readable error when the campaign cannot run through Exotel yet. */
    public void assertReady(Long userId, Campaign campaign) {
        ExotelClient.Creds c = settings.resolve(userId);
        if (c == null) {
            throw new IllegalStateException(
                    "Exotel is not set up. Add your Exotel details in Settings (or set EXOTEL_SID, EXOTEL_API_KEY, EXOTEL_API_TOKEN in .env).");
        }
        if (campaign.getType() == Campaign.CampaignType.SMS) {
            if (c.smsSender() == null || c.smsSender().isBlank()) {
                throw new IllegalStateException("Add an SMS sender (or Caller ID) in the Exotel settings.");
            }
        } else if (campaign.getType() == Campaign.CampaignType.CALL) {
            if (c.callerId() == null || c.callerId().isBlank()) {
                throw new IllegalStateException("Add a Caller ID (ExoPhone) in the Exotel settings.");
            }
            if (appIdFor(campaign, c) == null) {
                throw new IllegalStateException("This call campaign needs an Exotel app (flow) id, here or as the default in Settings.");
            }
        }
    }

    // ---------------------------------------------------------------- dispatch

    /** Sends the next due recipients. Called inside the campaign's transaction by CampaignRunService. */
    void dispatch(Campaign campaign, Long userId, int allowed, LocalDateTime now) {
        ExotelClient.Creds creds = settings.resolve(userId);
        if (creds == null) throw new IllegalStateException("Exotel is no longer set up");
        boolean sms = campaign.getType() == Campaign.CampaignType.SMS;

        List<CampaignRecipient> due = recipientRepository.findDue(
                campaign.getId(), RecipientStatus.PENDING, now, PageRequest.of(0, allowed));
        for (CampaignRecipient r : due) {
            r.setStatus(RecipientStatus.IN_PROGRESS);
            r.setAttempts(r.getAttempts() + 1);
            r.setLastDispatchedAt(now);
            r.setProviderSid(null);
            r.setHistoryId(null);
            String custom = "veylora-c" + campaign.getId() + "-r" + r.getId();
            try {
                if (sms) {
                    String body = CampaignRunService.render(campaign.getMessageBody(), r);
                    SmsMessage record = smsService.createSms(userId, r.getPhoneNumber(), body, campaign.getDltTemplateId(), null);
                    r.setHistoryId(record.getId());
                    String sid = client.sendSms(creds, r.getPhoneNumber(), body, campaign.getDltTemplateId(), custom);
                    r.setProviderSid(sid);
                    smsService.updateSmsStatus(userId, record.getId(), SmsMessage.SmsStatus.QUEUED, sid, null);
                } else {
                    CallLog entry = new CallLog();
                    entry.setPhoneNumber(r.getPhoneNumber());
                    entry.setCallType(CallLog.CallType.OUTGOING);
                    entry.setStatus(CallLog.CallStatus.INITIATED);
                    entry.setDurationSeconds(0);
                    r.setHistoryId(callLogService.createCallLog(userId, entry).getId());
                    r.setProviderSid(client.connectCall(creds, r.getPhoneNumber(), appIdFor(campaign, creds), custom));
                }
            } catch (ExotelClient.AuthException e) {
                throw e; // wrong credentials: the dispatcher pauses the campaign, nobody burns a retry
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                log.warn("Exotel send failed for campaign {} recipient {}: {}", campaign.getId(), r.getId(), error);
                recordHistory(userId, sms, r.getHistoryId(), false, null, error, null);
                finish(r, sms, false, null, true, error);
            }
        }
    }

    // --------------------------------------------------------------- reconcile

    /** Ids of attempts waiting for Exotel's final status. */
    public List<Long> openAttempts() {
        return recipientRepository.findAwaitingProvider(RecipientStatus.IN_PROGRESS).stream()
                .map(CampaignRecipient::getId).limit(MAX_CHECKS_PER_TICK).toList();
    }

    /** Asks Exotel how one attempt went and, when it is final, records the result. Run inside a transaction. */
    public void reconcileOne(Long recipientId) {
        CampaignRecipient r = recipientRepository.findById(recipientId).orElse(null);
        if (r == null || r.getStatus() != RecipientStatus.IN_PROGRESS || r.getProviderSid() == null) return;
        Campaign campaign = r.getCampaign();
        Long userId = campaign.getUser().getId();
        boolean sms = campaign.getType() == Campaign.CampaignType.SMS;
        ExotelClient.Creds creds = settings.resolve(userId);
        if (creds == null) return;

        ExotelClient.Status s;
        try {
            s = sms ? client.smsStatus(creds, r.getProviderSid()) : client.callStatus(creds, r.getProviderSid());
        } catch (Exception e) {
            log.debug("Could not read Exotel status of {}: {}", r.getProviderSid(), e.getMessage());
            return; // network hiccup: ask again next tick
        }

        String status = s.status() == null ? "" : s.status();
        if (sms) {
            if (status.equals("sent") || status.equals("delivered")) {
                recordHistory(userId, true, r.getHistoryId(), true, status.equals("delivered"), null, null);
                finish(r, true, true, null, true, null);
                return;
            }
            if (status.startsWith("failed")) {
                String error = "Exotel could not send the SMS (" + status + (s.detail() == null ? "" : ": " + s.detail()) + ")";
                recordHistory(userId, true, r.getHistoryId(), false, null, error, null);
                finish(r, true, false, null, !status.equals("failed_dnd"), error);
                return;
            }
        } else {
            switch (status) {
                case "completed" -> {
                    recordHistory(userId, false, r.getHistoryId(), true, null, null, s.durationSeconds());
                    finish(r, false, true, "COMPLETED", true, null);
                    return;
                }
                case "busy", "no-answer" -> {
                    recordHistory(userId, false, r.getHistoryId(), true, false, null, s.durationSeconds());
                    finish(r, false, true, "MISSED", true, null);
                    return;
                }
                case "failed", "canceled" -> {
                    String error = "Exotel could not place the call (" + status + ")";
                    recordHistory(userId, false, r.getHistoryId(), false, null, error, null);
                    finish(r, false, false, null, true, error);
                    return;
                }
                default -> { /* queued, ringing, in-progress: keep waiting */ }
            }
        }

        Duration limit = sms ? SMS_STUCK_AFTER : CALL_STUCK_AFTER;
        if (r.getLastDispatchedAt() != null && r.getLastDispatchedAt().plus(limit).isBefore(LocalDateTime.now())) {
            String error = "Exotel did not report a final status in time.";
            recordHistory(userId, sms, r.getHistoryId(), false, null, error, null);
            finish(r, sms, false, null, false, error);
        }
    }

    // ----------------------------------------------------------------- helpers

    /** Hands the result to the campaign, the same way a finished phone task does. */
    private void finish(CampaignRecipient r, boolean sms, boolean success, String callOutcome, boolean retryable, String error) {
        events.publishEvent(new TaskFinishedEvent(null, r.getId(),
                sms ? DeviceTask.TaskType.SMS : DeviceTask.TaskType.CALL, success, callOutcome, retryable, error));
    }

    /**
     * Keeps the SMS / call history row in step. success = it worked; flag = delivered (SMS) or not answered (call).
     */
    private void recordHistory(Long userId, boolean sms, Long historyId, boolean success, Boolean flag,
                               String error, Integer seconds) {
        if (historyId == null) return;
        try {
            if (sms) {
                SmsMessage.SmsStatus st = !success ? SmsMessage.SmsStatus.FAILED
                        : Boolean.TRUE.equals(flag) ? SmsMessage.SmsStatus.DELIVERED : SmsMessage.SmsStatus.SENT;
                SmsMessage current = smsService.getMessage(userId, historyId);
                smsService.updateSmsStatus(userId, historyId, st, current.getProviderMessageId(),
                        error == null ? null : error.substring(0, Math.min(error.length(), 500)));
            } else {
                CallLog.CallStatus st = !success ? CallLog.CallStatus.FAILED
                        : Boolean.FALSE.equals(flag) ? CallLog.CallStatus.MISSED : CallLog.CallStatus.COMPLETED;
                callLogService.updateCallStatus(userId, historyId, st, seconds);
            }
        } catch (Exception e) {
            log.warn("Could not update history row {}: {}", historyId, e.getMessage());
        }
    }

    private static String appIdFor(Campaign campaign, ExotelClient.Creds creds) {
        String own = campaign.getExotelAppId();
        if (own != null && !own.isBlank()) return own.trim();
        String def = creds.defaultAppId();
        return def == null || def.isBlank() ? null : def.trim();
    }
}
