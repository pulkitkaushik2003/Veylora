package com.example.veylora.Service;

import com.example.veylora.Exception.ResourceNotFoundException;
import com.example.veylora.Model.Campaign;
import com.example.veylora.Model.CampaignRecipient;
import com.example.veylora.Model.PersonalFile;
import com.example.veylora.Repository.CampaignRecipientRepository;
import com.example.veylora.Repository.PersonalFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * One file per person (for example a certificate). Upload a zip or many files whose names are the person's
 * email (rahul@x.com.pdf) or, when the names in the list are all different, the person's name (Rahul Sharma.pdf).
 */
@Service
public class PersonalFileService {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;
    private static final int MAX_FILES = 5000;

    private final CampaignService campaignService;
    private final PersonalFileRepository repository;
    private final CampaignRecipientRepository recipientRepository;

    public PersonalFileService(CampaignService campaignService, PersonalFileRepository repository,
                               CampaignRecipientRepository recipientRepository) {
        this.campaignService = campaignService;
        this.repository = repository;
        this.recipientRepository = recipientRepository;
    }

    public record UploadResult(int added, int replaced, List<String> skipped) {}

    public record MatchReport(int files, int recipients, int matched, int missingCount,
                              List<String> missingSample, List<String> unusedFiles) {}

    // ------------------------------------------------------------------ upload

    @Transactional
    public UploadResult upload(Long userId, Long campaignId, String originalName, byte[] data) {
        Campaign campaign = campaignService.getCampaign(userId, campaignId);
        if (campaign.getType() != Campaign.CampaignType.EMAIL) {
            throw new IllegalArgumentException("Personal files are only for email campaigns");
        }
        if (campaign.getStatus() != Campaign.CampaignStatus.DRAFT
                && campaign.getStatus() != Campaign.CampaignStatus.SCHEDULED
                && campaign.getStatus() != Campaign.CampaignStatus.PAUSED) {
            throw new IllegalStateException("Files can only be added to a draft, scheduled or paused campaign");
        }
        if (data == null || data.length == 0) throw new IllegalArgumentException("The file is empty");

        List<String> skipped = new ArrayList<>();
        int[] counts = new int[2]; // added, replaced
        long total = repository.totalSize(campaignId);
        long files = repository.countByCampaign_Id(campaignId);

        boolean zip = data.length > 3 && data[0] == 'P' && data[1] == 'K';
        List<Map.Entry<String, byte[]>> items = new ArrayList<>();
        if (zip) {
            try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry entry;
                int seen = 0;
                long unpacked = 0;
                while ((entry = in.getNextEntry()) != null) {
                    if (++seen > MAX_FILES) throw new IllegalArgumentException("The zip has too many files");
                    String name = entry.getName();
                    String base = baseName(name);
                    if (entry.isDirectory() || name.startsWith("__MACOSX") || base.startsWith(".")) continue;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        if (out.size() > MAX_FILE_BYTES) break;
                    }
                    if (out.size() > MAX_FILE_BYTES) { skipped.add(base + " (bigger than 5 MB)"); continue; }
                    unpacked += out.size();
                    if (unpacked > MAX_TOTAL_BYTES) throw new IllegalArgumentException("The zip is too big (max 100 MB unpacked)");
                    items.add(Map.entry(base, out.toByteArray()));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read the zip file");
            }
        } else {
            if (data.length > MAX_FILE_BYTES) throw new IllegalArgumentException("A file can be at most 5 MB");
            items.add(Map.entry(baseName(originalName == null ? "file" : originalName), data));
        }

        for (Map.Entry<String, byte[]> item : items) {
            String name = item.getKey();
            byte[] bytes = item.getValue();
            String type = CampaignAttachmentService.detectType(bytes);
            if (type == null) { skipped.add(name + " (only PDF, JPG and PNG are allowed)"); continue; }
            String key = norm(stripExtension(name));
            if (key.isEmpty()) { skipped.add(name + " (the file name has no usable letters)"); continue; }

            List<Long> old = repository.idsByKey(campaignId, key);
            long oldSize = 0;
            for (Long id : old) {
                oldSize += repository.findById(id).map(PersonalFile::getSizeBytes).orElse(0L);
                repository.deleteById(id);
            }
            if (total - oldSize + bytes.length > MAX_TOTAL_BYTES) throw new IllegalArgumentException("All personal files together can be at most 100 MB");
            if (old.isEmpty() && files + counts[0] >= MAX_FILES) throw new IllegalArgumentException("Too many files");
            total = total - oldSize + bytes.length;

            PersonalFile f = new PersonalFile();
            f.setCampaign(campaign);
            f.setFileName(name.length() > 255 ? name.substring(0, 255) : name);
            f.setMatchKey(key);
            f.setContentType(type);
            f.setSizeBytes(bytes.length);
            f.setData(bytes);
            repository.save(f);
            if (old.isEmpty()) counts[0]++; else counts[1]++;
        }
        return new UploadResult(counts[0], counts[1], skipped);
    }

    @Transactional(readOnly = true)
    public List<PersonalFileRepository.Info> list(Long userId, Long campaignId) {
        campaignService.getCampaign(userId, campaignId);
        return repository.listInfo(campaignId);
    }

    @Transactional
    public void delete(Long userId, Long campaignId, Long fileId) {
        requireEditable(campaignService.getCampaign(userId, campaignId));
        repository.delete(repository.findByIdAndCampaign_Id(fileId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found")));
    }

    @Transactional
    public void deleteAll(Long userId, Long campaignId) {
        requireEditable(campaignService.getCampaign(userId, campaignId));
        repository.deleteAllFor(campaignId);
    }

    private static void requireEditable(Campaign c) {
        if (c.getStatus() == Campaign.CampaignStatus.RUNNING) throw new IllegalStateException("Pause the campaign first");
    }

    // ---------------------------------------------------------------- matching

    public boolean hasFiles(Long campaignId) {
        return repository.countByCampaign_Id(campaignId) > 0;
    }

    @Transactional
    public MatchReport check(Long userId, Long campaignId) {
        return match(campaignService.getCampaign(userId, campaignId), false);
    }

    /**
     * Works out which file belongs to which recipient. A recipient matches a file whose simplified name equals the
     * simplified email, or equals the simplified name when that name is used by only one recipient.
     * With apply = true the result is saved on the recipients.
     */
    @Transactional
    public MatchReport match(Campaign campaign, boolean apply) {
        List<PersonalFileRepository.Info> infos = repository.listInfo(campaign.getId());
        Map<String, Long> byKey = new HashMap<>();
        for (PersonalFileRepository.Info i : infos) byKey.put(i.getMatchKey(), i.getId());

        List<CampaignRecipient> recipients = recipientRepository.findByCampaign_IdOrderByIdAsc(campaign.getId());
        Map<String, Integer> nameCount = new HashMap<>();
        for (CampaignRecipient r : recipients) {
            String n = norm(r.getName());
            if (!n.isEmpty()) nameCount.merge(n, 1, Integer::sum);
        }

        Set<Long> used = new HashSet<>();
        List<String> missing = new ArrayList<>();
        int matched = 0;
        int missingCount = 0;
        for (CampaignRecipient r : recipients) {
            Long fileId = byKey.get(norm(r.getEmail()));
            String nameKey = norm(r.getName());
            if (fileId == null && !nameKey.isEmpty() && nameCount.get(nameKey) == 1) fileId = byKey.get(nameKey);
            if (apply) r.setPersonalFileId(fileId);
            if (fileId != null) {
                matched++;
                used.add(fileId);
            } else {
                missingCount++;
                if (missing.size() < 20) missing.add(r.getEmail() != null ? r.getEmail() : String.valueOf(r.getName()));
            }
        }
        List<String> unused = new ArrayList<>();
        for (PersonalFileRepository.Info i : infos) {
            if (!used.contains(i.getId()) && unused.size() < 50) unused.add(i.getFileName());
        }
        return new MatchReport(infos.size(), recipients.size(), matched, missingCount, missing, unused);
    }

    // ----------------------------------------------------------------- helpers

    private static String baseName(String path) {
        String p = path.replace('\\', '/');
        return p.substring(p.lastIndexOf('/') + 1);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Lower case, only letters and digits (any language). "Rahul.Sharma@x.com" and "rahul sharma x com" match. */
    static String norm(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }
}
