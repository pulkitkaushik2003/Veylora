package com.example.veylora.Service;

import com.example.veylora.Exception.ResourceNotFoundException;
import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.CampaignAttachment;
import com.example.veylora.Repository.CampaignAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** Files that go out with the emails of a campaign. Only PDF, JPG and PNG, checked by content and not just by name. */
@Service
public class CampaignAttachmentService {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 10L * 1024 * 1024;
    public static final int MAX_FILES = 5;

    private final CampaignService campaignService;
    private final CampaignAttachmentRepository repository;

    public CampaignAttachmentService(CampaignService campaignService, CampaignAttachmentRepository repository) {
        this.campaignService = campaignService;
        this.repository = repository;
    }

    @Transactional
    public CampaignAttachmentRepository.Info add(Long userId, Long campaignId, String originalName, byte[] data) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getType() != Campaign.CampaignType.EMAIL) {
            throw new IllegalArgumentException("Attachments are only for email campaigns");
        }
        if (!EnumSet.of(Campaign.CampaignStatus.DRAFT, Campaign.CampaignStatus.SCHEDULED,
                Campaign.CampaignStatus.PAUSED).contains(campaign.getStatus())) {
            throw new IllegalStateException("Files can only be added to a draft, scheduled or paused campaign");
        }
        if (data == null || data.length == 0) throw new IllegalArgumentException("The file is empty");
        if (data.length > MAX_FILE_BYTES) throw new IllegalArgumentException("A file can be at most 5 MB");
        if (repository.countByCampaign_Id(campaignId) >= MAX_FILES) {
            throw new IllegalArgumentException("A campaign can have at most " + MAX_FILES + " files");
        }
        if (repository.totalSize(campaignId) + data.length > MAX_TOTAL_BYTES) {
            throw new IllegalArgumentException("All files together can be at most 10 MB");
        }

        String type = detectType(data);
        if (type == null) throw new IllegalArgumentException("Only PDF, JPG and PNG files are allowed");

        CampaignAttachment attachment = new CampaignAttachment();
        attachment.setCampaign(campaign);
        attachment.setFileName(cleanName(originalName, type));
        attachment.setContentType(type);
        attachment.setSizeBytes(data.length);
        attachment.setData(data);
        CampaignAttachment saved = repository.save(attachment);
        return repository.listInfo(campaignId).stream()
                .filter(i -> i.getId().equals(saved.getId())).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<CampaignAttachmentRepository.Info> list(Long userId, Long campaignId) {
        campaignService.getCampaign(userId, campaignId); // ownership check
        return repository.listInfo(campaignId);
    }

    @Transactional
    public void delete(Long userId, Long campaignId, Long attachmentId) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getStatus() == Campaign.CampaignStatus.RUNNING) {
            throw new IllegalStateException("Pause the campaign before removing files");
        }
        repository.delete(repository.findByIdAndCampaign_Id(attachmentId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found")));
    }

    /** Looks at the first bytes of the file, so a renamed .exe cannot pass as a PDF. */
    public static String detectType(byte[] d) {
        if (d.length > 4 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F') return "application/pdf";
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "image/jpeg";
        return null;
    }

    private static String cleanName(String original, String type) {
        String ext = switch (type) { case "application/pdf" -> ".pdf"; case "image/png" -> ".png"; default -> ".jpg"; };
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.isEmpty()) name = "attachment";
        if (name.length() > 150) name = name.substring(0, 150);
        String lower = name.toLowerCase(Locale.ROOT);
        boolean hasExt = lower.endsWith(".pdf") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        return hasExt ? name : name + ext;
    }
}
