package com.example.veylora.Service;

import com.example.veylora.Model.CallLog;
import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.Contact;
import com.example.veylora.Model.Device;
import com.example.veylora.Model.EmailMessage;
import com.example.veylora.Model.SmsMessage;
import com.example.veylora.Repository.CallLogRepository;
import com.example.veylora.Repository.CampaignRepository;
import com.example.veylora.Repository.ContactRepository;
import com.example.veylora.Repository.DeviceRepository;
import com.example.veylora.Repository.EmailMessageRepository;
import com.example.veylora.Repository.SmsMessageRepository;
import com.example.veylora.dto.DashboardActivityItem;
import com.example.veylora.dto.DashboardChannelStat;
import com.example.veylora.dto.DashboardSummaryResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
public class DashboardService {

    private final ContactRepository contactRepository;
    private final CallLogRepository callLogRepository;
    private final SmsMessageRepository smsMessageRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final CampaignRepository campaignRepository;
    private final DeviceRepository deviceRepository;

    public DashboardService(
            ContactRepository contactRepository,
            CallLogRepository callLogRepository,
            SmsMessageRepository smsMessageRepository,
            EmailMessageRepository emailMessageRepository,
            CampaignRepository campaignRepository,
            DeviceRepository deviceRepository) {
        this.contactRepository = contactRepository;
        this.callLogRepository = callLogRepository;
        this.smsMessageRepository = smsMessageRepository;
        this.emailMessageRepository = emailMessageRepository;
        this.campaignRepository = campaignRepository;
        this.deviceRepository = deviceRepository;
    }

    public DashboardSummaryResponse getSummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<Contact> contacts = contactRepository.findByUser_Id(userId);
        List<CallLog> calls = callLogRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        List<SmsMessage> smsMessages = smsMessageRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        List<EmailMessage> emailMessages = emailMessageRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        List<Campaign> campaigns = campaignRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        List<Device> devices = deviceRepository.findByUser_Id(userId);

        long callsToday = calls.stream()
                .filter(call -> call.getCreatedAt() != null && call.getCreatedAt().toLocalDate().equals(today))
                .count();
        long answeredCalls = calls.stream()
                .filter(call -> call.getStatus() == CallLog.CallStatus.ANSWERED || call.getStatus() == CallLog.CallStatus.COMPLETED)
                .count();
        long missedCalls = calls.stream()
                .filter(call -> call.getStatus() == CallLog.CallStatus.MISSED)
                .count();
        long failedCalls = calls.stream()
                .filter(call -> call.getStatus() == CallLog.CallStatus.FAILED)
                .count();

        long smsSubmitted = smsMessages.stream()
                .filter(message -> message.getCreatedAt() != null && message.getCreatedAt().toLocalDate().equals(today))
                .count();
        long smsDelivered = smsMessages.stream()
                .filter(message -> message.getStatus() == SmsMessage.SmsStatus.DELIVERED)
                .count();
        long smsFailed = smsMessages.stream()
                .filter(message -> message.getStatus() == SmsMessage.SmsStatus.FAILED)
                .count();

        long emailsAccepted = emailMessages.stream()
                .filter(message -> message.getStatus() == EmailMessage.EmailStatus.SENT || message.getStatus() == EmailMessage.EmailStatus.DELIVERED)
                .count();
        long emailsBounced = 0L;
        long emailsFailed = emailMessages.stream()
                .filter(message -> message.getStatus() == EmailMessage.EmailStatus.FAILED)
                .count();

        long activeCampaigns = campaigns.stream()
                .filter(campaign -> campaign.getStatus() != Campaign.CampaignStatus.COMPLETED
                        && campaign.getStatus() != Campaign.CampaignStatus.FAILED)
                .count();

        long onlineDevices = devices.stream()
                .filter(device -> device.getStatus() == Device.DeviceStatus.ONLINE)
                .count();

        List<DashboardActivityItem> recentActivity = new ArrayList<>();
        var activities = new ArrayList<Object>();

        for (CallLog call : calls) {
            if (call.getCreatedAt() == null) {
                continue;
            }
            activities.add(new DashboardActivityItem(call.getCreatedAt(), "CALL", "Call update", call.getPhoneNumber() + " / " + call.getStatus()));
        }
        for (SmsMessage message : smsMessages) {
            if (message.getCreatedAt() == null) {
                continue;
            }
            activities.add(new DashboardActivityItem(message.getCreatedAt(), "SMS", "SMS update", message.getRecipientPhone() + " / " + message.getStatus()));
        }
        for (EmailMessage message : emailMessages) {
            if (message.getCreatedAt() == null) {
                continue;
            }
            activities.add(new DashboardActivityItem(message.getCreatedAt(), "EMAIL", "Email update", message.getRecipientEmail() + " / " + message.getStatus()));
        }

        activities.sort(Comparator.comparing(item -> ((DashboardActivityItem) item).timestamp(), Comparator.reverseOrder()));
        for (Object activity : activities.stream().limit(10).toList()) {
            recentActivity.add((DashboardActivityItem) activity);
        }

        List<DashboardChannelStat> channelStats = new ArrayList<>();
        long callSubmit = calls.stream().filter(call -> call.getCreatedAt() != null && call.getCreatedAt().toLocalDate().equals(today)).count();
        long callSuccess = answeredCalls;
        channelStats.add(new DashboardChannelStat("CALL", callSubmit, callSuccess, failedCalls, callSubmit == 0 ? 0 : (double) callSuccess / callSubmit * 100));

        long smsSubmit = smsSubmitted;
        long smsSuccess = smsDelivered;
        channelStats.add(new DashboardChannelStat("SMS", smsSubmit, smsSuccess, smsFailed, smsSubmit == 0 ? 0 : (double) smsSuccess / smsSubmit * 100));

        long emailSubmit = emailMessages.size();
        long emailSuccess = emailsAccepted;
        channelStats.add(new DashboardChannelStat("EMAIL", emailSubmit, emailSuccess, emailsFailed, emailSubmit == 0 ? 0 : (double) emailSuccess / emailSubmit * 100));

        return new DashboardSummaryResponse(
                contacts.size(),
                callsToday,
                answeredCalls,
                missedCalls,
                failedCalls,
                smsSubmitted,
                smsDelivered,
                smsFailed,
                emailsAccepted,
                emailsBounced,
                emailsFailed,
                activeCampaigns,
                onlineDevices,
                devices.size(),
                now,
                recentActivity,
                channelStats
        );
    }

    public List<DashboardActivityItem> getRecentActivity(Long userId) {
        return getSummary(userId).recentActivity();
    }
}
