package com.example.veylora.Service;

import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.CampaignRecipient;
import com.example.veylora.Model.CampaignRecipient.RecipientStatus;
import com.example.veylora.Model.CampaignAttachment;
import com.example.veylora.Model.Device;
import com.example.veylora.Model.PersonalFile;
import com.example.veylora.Model.EmailMessage;
import com.example.veylora.Model.DeviceTask;
import com.example.veylora.Repository.CampaignAttachmentRepository;
import com.example.veylora.Repository.CampaignRecipientRepository;
import com.example.veylora.Repository.PersonalFileRepository;
import com.example.veylora.Repository.CampaignRepository;
import com.example.veylora.Repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs campaigns: keeps a list of recipients, feeds them to the phone as device tasks
 * at the allowed speed, handles pause / resume / cancel, and retries failures.
 */
@Service
public class CampaignRunService {

    private static final Logger log = LoggerFactory.getLogger(CampaignRunService.class);

    /** Messages or calls per minute when the campaign does not say. Kept low on purpose to protect the SIM. */
    private static final int DEFAULT_RATE_PER_MINUTE = 5;
    private static final int MAX_RATE_PER_MINUTE = 60;
    private static final int MAX_RECIPIENTS = 5000;

    private final CampaignService campaignService;
    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository recipientRepository;
    private final ContactRepository contactRepository;
    private final DeviceService deviceService;
    private final DeviceTaskService deviceTaskService;
    private final EmailMessageService emailMessageService;
    private final EmailSenderService emailSender;
    private final CampaignAttachmentRepository attachmentRepository;
    private final PersonalFileRepository personalFileRepository;
    private final PersonalFileService personalFileService;
    private final ExotelCampaignService exotelService;

    public CampaignRunService(CampaignService campaignService,
                              CampaignRepository campaignRepository,
                              CampaignRecipientRepository recipientRepository,
                              ContactRepository contactRepository,
                              DeviceService deviceService,
                              DeviceTaskService deviceTaskService,
                              EmailMessageService emailMessageService,
                              EmailSenderService emailSender,
                              CampaignAttachmentRepository attachmentRepository,
                              PersonalFileRepository personalFileRepository,
                              PersonalFileService personalFileService,
                              ExotelCampaignService exotelService) {
        this.campaignService = campaignService;
        this.campaignRepository = campaignRepository;
        this.recipientRepository = recipientRepository;
        this.contactRepository = contactRepository;
        this.deviceService = deviceService;
        this.deviceTaskService = deviceTaskService;
        this.emailMessageService = emailMessageService;
        this.emailSender = emailSender;
        this.attachmentRepository = attachmentRepository;
        this.personalFileRepository = personalFileRepository;
        this.personalFileService = personalFileService;
        this.exotelService = exotelService;
    }

    public record RecipientInput(String name, String phoneNumber, String email) {}

    public record AddResult(int added, int duplicates, int invalid, int total) {}

    // ------------------------------------------------------------ user actions

    @Transactional
    public Campaign setMessageBody(Long userId, Long campaignId, String messageBody) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (messageBody != null && messageBody.length() > 1000) {
            throw new IllegalArgumentException("Message text is too long (max 1000 characters)");
        }
        campaign.setMessageBody(messageBody == null || messageBody.isBlank() ? null : messageBody);
        return campaignRepository.save(campaign);
    }

    /**
     * Chooses who sends: the paired phone (default) or the user's Exotel account. For Exotel, appId is the
     * flow that runs when a call is answered and dltTemplateId is the approved DLT template of the SMS.
     */
    @Transactional
    public Campaign setProvider(Long userId, Long campaignId, String provider, String appId, String dltTemplateId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        String p = provider == null || provider.isBlank() ? "PHONE" : provider.trim().toUpperCase();
        if (!p.equals("PHONE") && !p.equals("EXOTEL")) throw new IllegalArgumentException("Provider must be PHONE or EXOTEL");
        if (p.equals("EXOTEL") && campaign.getType() == Campaign.CampaignType.EMAIL) {
            throw new IllegalArgumentException("Email campaigns cannot use Exotel");
        }
        String app = appId == null ? "" : appId.trim();
        if (!app.isEmpty() && !app.matches("[0-9]{1,20}")) {
            throw new IllegalArgumentException("Exotel app id is a number, for example 1234567");
        }
        String dlt = dltTemplateId == null ? "" : dltTemplateId.trim();
        if (dlt.length() > 60) throw new IllegalArgumentException("DLT template id is too long");
        boolean exotel = p.equals("EXOTEL");
        campaign.setProvider(exotel ? "EXOTEL" : null);
        campaign.setExotelAppId(exotel && !app.isEmpty() ? app : null);
        campaign.setDltTemplateId(exotel && !dlt.isEmpty() ? dlt : null);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign setEmailSubject(Long userId, Long campaignId, String subject) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (subject != null && subject.length() > 300) {
            throw new IllegalArgumentException("Subject is too long (max 300 characters)");
        }
        campaign.setEmailSubject(subject == null || subject.isBlank() ? null : subject.trim());
        return campaignRepository.save(campaign);
    }

    @Transactional
    public AddResult addRecipients(Long userId, Long campaignId,
                                   List<RecipientInput> inputs, List<Long> contactIds) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (!EnumSet.of(Campaign.CampaignStatus.DRAFT, Campaign.CampaignStatus.SCHEDULED,
                Campaign.CampaignStatus.PAUSED).contains(campaign.getStatus())) {
            throw new IllegalStateException("Recipients can only be added to a draft, scheduled or paused campaign");
        }

        List<RecipientInput> all = new ArrayList<>();
        if (inputs != null) all.addAll(inputs);
        if (contactIds != null) {
            for (Long contactId : contactIds) {
                contactRepository.findByIdAndUser_Id(contactId, userId)
                        .ifPresent(c -> all.add(new RecipientInput(c.getName(), c.getPhone(), c.getEmail())));
            }
        }

        Set<String> existing = new HashSet<>(recipientRepository.findPhones(campaignId));
        int added = 0;
        int duplicates = 0;
        int invalid = 0;
        boolean emailCampaign = campaign.getType() == Campaign.CampaignType.EMAIL;
        for (RecipientInput input : all) {
            String address = null;
            String phone = null;
            if (input != null && emailCampaign) {
                address = normalizeEmail(input.email() != null ? input.email() : input.phoneNumber());
                phone = address == null ? null : emailKey(address);
            } else if (input != null) {
                phone = normalizePhone(input.phoneNumber());
            }
            if (phone == null) {
                invalid++;
                continue;
            }
            if (!existing.add(phone)) {
                duplicates++;
                continue;
            }
            if (existing.size() > MAX_RECIPIENTS) {
                throw new IllegalArgumentException("A campaign can have at most " + MAX_RECIPIENTS + " recipients");
            }
            CampaignRecipient recipient = new CampaignRecipient();
            recipient.setCampaign(campaign);
            recipient.setPhoneNumber(phone);
            recipient.setEmail(address);
            String name = input.name() == null ? null : input.name().trim();
            recipient.setName(name == null || name.isEmpty() ? null : name.substring(0, Math.min(name.length(), 100)));
            recipientRepository.save(recipient);
            added++;
        }

        int total = (int) recipientRepository.countByCampaign_Id(campaignId);
        campaign.setTotalRecipients(total);
        campaignRepository.save(campaign);
        return new AddResult(added, duplicates, invalid, total);
    }

    @Transactional(readOnly = true)
    public List<CampaignRecipient> listRecipients(Long userId, Long campaignId) {
        campaignService.getCampaign(userId, campaignId); // ownership check
        return recipientRepository.findByCampaign_IdOrderByIdAsc(campaignId);
    }

    @Transactional
    public Campaign start(Long userId, Long campaignId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getStatus() != Campaign.CampaignStatus.DRAFT
                && campaign.getStatus() != Campaign.CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Only a draft or scheduled campaign can be started");
        }
        markRunning(campaign);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign pause(Long userId, Long campaignId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getStatus() != Campaign.CampaignStatus.RUNNING) {
            throw new IllegalStateException("Only a running campaign can be paused");
        }
        campaign.setStatus(Campaign.CampaignStatus.PAUSED);
        releaseQueuedTasks(campaign, false);
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign resume(Long userId, Long campaignId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getStatus() != Campaign.CampaignStatus.PAUSED) {
            throw new IllegalStateException("Only a paused campaign can be resumed");
        }
        markRunning(campaign);
        return campaignRepository.save(campaign);
    }

    /**
     * Stops the campaign for good. There is no CANCELLED status in the campaigns table, so a
     * cancelled campaign ends as COMPLETED, and the recipients that never ran show as CANCELLED.
     */
    @Transactional
    public Campaign cancel(Long userId, Long campaignId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getStatus() == Campaign.CampaignStatus.COMPLETED
                || campaign.getStatus() == Campaign.CampaignStatus.FAILED) {
            throw new IllegalStateException("This campaign has already finished");
        }
        releaseQueuedTasks(campaign, true);
        campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
        campaign.setCompletedAt(LocalDateTime.now());
        return campaignRepository.save(campaign);
    }

    /** Puts every FAILED recipient back in line, with a fresh set of attempts. */
    @Transactional
    public Campaign retryFailed(Long userId, Long campaignId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        List<CampaignRecipient> failed = recipientRepository
                .findByCampaign_IdAndStatus(campaignId, RecipientStatus.FAILED);
        if (failed.isEmpty()) {
            throw new IllegalStateException("There are no failed recipients to retry");
        }
        for (CampaignRecipient recipient : failed) {
            recipient.setStatus(RecipientStatus.PENDING);
            recipient.setAttempts(0);
            recipient.setNextAttemptAt(null);
            recipient.setLastError(null);
        }
        campaign.setFailedCount(Math.max(0, campaign.getFailedCount() - failed.size()));
        if (campaign.getStatus() == Campaign.CampaignStatus.COMPLETED
                || campaign.getStatus() == Campaign.CampaignStatus.FAILED) {
            markRunning(campaign);
            campaign.setCompletedAt(null);
        }
        return campaignRepository.save(campaign);
    }

    // -------------------------------------------------------------- dispatcher

    /** Called every few seconds for each running or scheduled campaign (see CampaignDispatcher). */
    public void dispatchOne(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) return;
        LocalDateTime now = LocalDateTime.now();

        if (campaign.getStatus() == Campaign.CampaignStatus.SCHEDULED) {
            if (campaign.getScheduledAt() == null || campaign.getScheduledAt().isAfter(now)) return;
            markRunning(campaign);
        }
        if (campaign.getStatus() != Campaign.CampaignStatus.RUNNING) return;

        Long userId = campaign.getUser().getId();
        boolean isEmail = campaign.getType() == Campaign.CampaignType.EMAIL;
        Long deviceId = (isEmail || campaign.isExotel()) ? null : Long.valueOf(campaign.getSelectedDeviceId());
        int limit = ratePerMinute(campaign);

        long recent = recipientRepository.countByCampaign_IdAndLastDispatchedAtAfter(campaignId, now.minusSeconds(60));
        long outstanding = recipientRepository.countByCampaign_IdAndStatus(campaignId, RecipientStatus.IN_PROGRESS);
        long allowed = Math.min(limit - recent, limit - outstanding);

        if (allowed > 0 && isEmail) {
            dispatchEmails(campaign, userId, (int) allowed, now);
        } else if (allowed > 0 && campaign.isExotel()) {
            exotelService.dispatch(campaign, userId, (int) allowed, now);
        } else if (allowed > 0) {
            DeviceTask.TaskType type = campaign.getType() == Campaign.CampaignType.SMS
                    ? DeviceTask.TaskType.SMS : DeviceTask.TaskType.CALL;
            Integer simSlotIndex = campaign.getSelectedSimSlot() == null ? null : campaign.getSelectedSimSlot() - 1;

            List<CampaignRecipient> due = recipientRepository.findDue(
                    campaignId, RecipientStatus.PENDING, now, PageRequest.of(0, (int) allowed));
            for (CampaignRecipient recipient : due) {
                String body = type == DeviceTask.TaskType.SMS ? render(campaign.getMessageBody(), recipient) : null;
                DeviceTask task = deviceTaskService.createForRecipient(
                        userId, deviceId, type, recipient.getPhoneNumber(), body, simSlotIndex, recipient.getId());
                recipient.setStatus(RecipientStatus.IN_PROGRESS);
                recipient.setAttempts(recipient.getAttempts() + 1);
                recipient.setLastDispatchedAt(now);
                recipient.setDeviceTaskId(task.getId());
            }
        }

        long open = recipientRepository.countByCampaign_IdAndStatusIn(
                campaignId, List.of(RecipientStatus.PENDING, RecipientStatus.IN_PROGRESS));
        if (open == 0) {
            campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
            campaign.setCompletedAt(now);
        }
        campaignRepository.save(campaign);
    }

    /** Something went wrong while dispatching (for example the phone was disabled): stop and wait. */
    public void pauseAfterError(Long campaignId, String reason) {
        campaignRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setStatus(Campaign.CampaignStatus.PAUSED);
            campaignRepository.save(campaign);
            log.warn("Campaign {} paused: {}", campaignId, reason);
        });
    }

    // ----------------------------------------------------- results from phone

    /** A task made for a campaign recipient finished (or was given up on). */
    @EventListener
    public void onTaskFinished(TaskFinishedEvent event) {
        if (event.campaignRecipientId() == null) return;
        CampaignRecipient recipient = recipientRepository.findById(event.campaignRecipientId()).orElse(null);
        if (recipient == null || recipient.getStatus() != RecipientStatus.IN_PROGRESS) return;

        boolean notAnswered = event.success()
                && event.type() == DeviceTask.TaskType.CALL
                && "MISSED".equals(event.callOutcome());
        applyOutcome(recipient, recipient.getCampaign(), event.success(), notAnswered,
                event.retryable(), event.error());
    }

    /** Decides: done, retry later, or failed. Shared by phone tasks and emails. */
    private void applyOutcome(CampaignRecipient recipient, Campaign campaign, boolean success,
                              boolean notAnswered, boolean retryable, String error) {
        boolean campaignClosed = campaign.getStatus() == Campaign.CampaignStatus.COMPLETED
                || campaign.getStatus() == Campaign.CampaignStatus.FAILED;

        if (success && !notAnswered) {
            recipient.setStatus(RecipientStatus.DONE);
            recipient.setLastError(null);
            campaign.setSuccessfulCount(campaign.getSuccessfulCount() + 1);
            return;
        }

        boolean mayRetry = !campaignClosed && canRetry(recipient, campaign)
                && (notAnswered ? Boolean.TRUE.equals(campaign.getRetryOnNoAnswer()) : retryable);
        String reason = notAnswered ? "The call was not answered." : error;
        recipient.setLastError(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)));

        if (mayRetry) {
            int delay = campaign.getRetryDelayMinutes() == null ? 1 : campaign.getRetryDelayMinutes();
            recipient.setStatus(RecipientStatus.PENDING);
            recipient.setNextAttemptAt(LocalDateTime.now().plusMinutes(delay));
        } else {
            recipient.setStatus(RecipientStatus.FAILED);
            campaign.setFailedCount(campaign.getFailedCount() + 1);
        }
    }

    /** Sends the next emails directly from the backend (no phone involved). */
    private void dispatchEmails(Campaign campaign, Long userId, int allowed, LocalDateTime now) {
        List<CampaignRecipient> due = recipientRepository.findDue(
                campaign.getId(), RecipientStatus.PENDING, now, PageRequest.of(0, allowed));
        List<CampaignAttachment> files = due.isEmpty() ? List.of()
                : attachmentRepository.findByCampaign_IdOrderByIdAsc(campaign.getId());
        for (CampaignRecipient recipient : due) {
            recipient.setAttempts(recipient.getAttempts() + 1);
            recipient.setLastDispatchedAt(now);
            String subject = render(campaign.getEmailSubject(), recipient);
            String body = render(campaign.getMessageBody(), recipient);
            EmailMessage record = null;
            try {
                record = emailMessageService.createEmail(userId, recipient.getEmail(), subject, body);
                List<CampaignAttachment> toSend = files;
                if (recipient.getPersonalFileId() != null) {
                    PersonalFile personal = personalFileRepository.findById(recipient.getPersonalFileId())
                            .orElseThrow(() -> new IllegalStateException("This person's file was removed"));
                    CampaignAttachment own = new CampaignAttachment();
                    own.setFileName(personal.getFileName());
                    own.setContentType(personal.getContentType());
                    own.setData(personal.getData());
                    toSend = new java.util.ArrayList<>(files);
                    toSend.add(own);
                }
                emailSender.send(userId, recipient.getEmail(), subject, body, toSend);
                emailMessageService.updateEmailStatus(userId, record.getId(), EmailMessage.EmailStatus.SENT, null, null);
                applyOutcome(recipient, campaign, true, false, true, null);
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (record != null) {
                    try {
                        emailMessageService.updateEmailStatus(userId, record.getId(),
                                EmailMessage.EmailStatus.FAILED, null, error.substring(0, Math.min(error.length(), 1000)));
                    } catch (Exception ignored) {
                        // the recipient result below is what matters
                    }
                }
                applyOutcome(recipient, campaign, false, false, true, error);
            }
        }
    }

    // ----------------------------------------------------------------- helpers

    private static boolean canRetry(CampaignRecipient recipient, Campaign campaign) {
        int retries = campaign.getMaxRetries() == null ? 0 : campaign.getMaxRetries();
        return recipient.getAttempts() < 1 + retries;
    }

    private static int ratePerMinute(Campaign campaign) {
        Integer rate = campaign.getRateLimitPerMinute();
        if (rate == null) return DEFAULT_RATE_PER_MINUTE;
        return Math.max(1, Math.min(rate, MAX_RATE_PER_MINUTE));
    }

    static String render(String template, CampaignRecipient recipient) {
        if (template == null) return "";
        String name = recipient.getName() == null ? "" : recipient.getName();
        boolean email = recipient.getEmail() != null;
        return template.replace("{{name}}", name)
                .replace("{{email}}", email ? recipient.getEmail() : "")
                .replace("{{phone}}", email ? "" : recipient.getPhoneNumber());
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return null;
        String email = raw.trim().toLowerCase();
        return email.length() <= 254 && email.matches("[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]{2,}") ? email : null;
    }

    /** Short unique key for an address, because the phoneNumber column is only 20 characters long. */
    private static String emailKey(String email) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("E");
            for (int i = 0; i < 9; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        String phone = raw.replaceAll("[\\s\\-().]", "");
        return phone.matches("\\+?[0-9]{7,15}") ? phone : null;
    }

    /** Checks the campaign can really run, picks the phone, and flips it to RUNNING. */
    private void markRunning(Campaign campaign) {
        boolean isEmail = campaign.getType() == Campaign.CampaignType.EMAIL;
        if (isEmail && (campaign.getEmailSubject() == null || campaign.getEmailSubject().isBlank())) {
            throw new IllegalArgumentException("An email campaign needs a subject");
        }
        if ((isEmail || campaign.getType() == Campaign.CampaignType.SMS)
                && (campaign.getMessageBody() == null || campaign.getMessageBody().isBlank())) {
            throw new IllegalArgumentException("This campaign needs a message text");
        }
        if (recipientRepository.countByCampaign_Id(campaign.getId()) == 0) {
            throw new IllegalArgumentException("Add recipients before starting the campaign");
        }
        if (isEmail) {
            if (personalFileService.hasFiles(campaign.getId())) {
                PersonalFileService.MatchReport report = personalFileService.match(campaign, true);
                if (report.missingCount() > 0) {
                    throw new IllegalArgumentException(report.missingCount() + " of " + report.recipients()
                            + " recipients have no file, for example: " + String.join(", ", report.missingSample().subList(0, Math.min(5, report.missingSample().size())))
                            + ". Name each file like the person's email (rahul@x.com.pdf) and upload again, or remove those recipients.");
                }
            }
            if (!emailSender.isConfigured(campaign.getUser().getId())) {
                throw new IllegalStateException(
                        "Email is not set up. Add your email and app password in Settings (or set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD in .env).");
            }
            campaign.setStatus(Campaign.CampaignStatus.RUNNING);
            if (campaign.getStartedAt() == null) campaign.setStartedAt(LocalDateTime.now());
            return;
        }

        Long userId = campaign.getUser().getId();
        if (campaign.isExotel()) {
            exotelService.assertReady(userId, campaign);
            campaign.setStatus(Campaign.CampaignStatus.RUNNING);
            if (campaign.getStartedAt() == null) campaign.setStartedAt(LocalDateTime.now());
            return;
        }
        Device device;
        String selected = campaign.getSelectedDeviceId();
        if (selected == null || selected.isBlank()) {
            List<Device> usable = deviceService.getUserDevices(userId).stream()
                    .filter(d -> d.getStatus() != Device.DeviceStatus.DISABLED)
                    .toList();
            if (usable.size() != 1) {
                throw new IllegalArgumentException("Choose which phone should run this campaign");
            }
            device = usable.get(0);
            campaign.setSelectedDeviceId(String.valueOf(device.getId()));
        } else {
            try {
                device = deviceService.getDevice(userId, Long.valueOf(selected));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The selected phone is not valid");
            }
        }
        if (device.getStatus() == Device.DeviceStatus.DISABLED) {
            throw new IllegalStateException("The selected phone is disabled");
        }

        campaign.setStatus(Campaign.CampaignStatus.RUNNING);
        if (campaign.getStartedAt() == null) campaign.setStartedAt(LocalDateTime.now());
    }

    /**
     * Takes back tasks the phone has not picked up yet. Tasks already taken by the phone cannot be
     * recalled, their result will still arrive. cancel = true marks the waiting recipients CANCELLED,
     * otherwise they go back in line to be sent after a resume.
     */
    private void releaseQueuedTasks(Campaign campaign, boolean cancel) {
        for (CampaignRecipient recipient : recipientRepository
                .findByCampaign_IdAndStatus(campaign.getId(), RecipientStatus.IN_PROGRESS)) {
            if (recipient.getDeviceTaskId() != null && deviceTaskService.cancelIfPending(recipient.getDeviceTaskId())) {
                if (cancel) {
                    recipient.setStatus(RecipientStatus.CANCELLED);
                } else {
                    recipient.setStatus(RecipientStatus.PENDING);
                    recipient.setAttempts(Math.max(0, recipient.getAttempts() - 1));
                    recipient.setNextAttemptAt(null);
                }
            }
        }
        if (cancel) {
            for (CampaignRecipient recipient : recipientRepository
                    .findByCampaign_IdAndStatus(campaign.getId(), RecipientStatus.PENDING)) {
                recipient.setStatus(RecipientStatus.CANCELLED);
            }
        }
    }
}
