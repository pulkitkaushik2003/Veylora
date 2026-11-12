package com.example.veylora.Model;

import jakarta.persistence.*;

/** "This email gets that file" (from the document list the owner uploads: email + file name). Both are stored simplified. */
@Entity
@Table(name = "personal_file_map", indexes = @Index(name = "idx_pfmap_campaign", columnList = "campaignId"))
public class PersonalFileMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false, length = 255)
    private String emailKey;

    @Column(nullable = false, length = 255)
    private String fileKey;

    protected PersonalFileMap() {}

    public PersonalFileMap(Long campaignId, String emailKey, String fileKey) {
        this.campaignId = campaignId;
        this.emailKey = emailKey;
        this.fileKey = fileKey;
    }

    public Long getCampaignId() { return campaignId; }
    public String getEmailKey() { return emailKey; }
    public String getFileKey() { return fileKey; }
}
