package com.example.veylora.Controller;

import com.example.veylora.Repository.CampaignAttachmentRepository;
import com.example.veylora.Service.CampaignAttachmentService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/attachments")
public class CampaignAttachmentController {

    private final CampaignAttachmentService service;

    public CampaignAttachmentController(CampaignAttachmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<AttachmentResponse> upload(@CurrentUserId Long userId, @PathVariable Long campaignId,
                                                     @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(
                service.add(userId, campaignId, file.getOriginalFilename(), file.getBytes())));
    }

    @GetMapping
    public List<AttachmentResponse> list(@CurrentUserId Long userId, @PathVariable Long campaignId) {
        return service.list(userId, campaignId).stream().map(CampaignAttachmentController::toResponse).toList();
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long campaignId,
                                       @PathVariable Long attachmentId) {
        service.delete(userId, campaignId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    private static AttachmentResponse toResponse(CampaignAttachmentRepository.Info i) {
        return new AttachmentResponse(i.getId(), i.getFileName(), i.getContentType(), i.getSizeBytes());
    }

    public record AttachmentResponse(Long id, String fileName, String contentType, long sizeBytes) {}
}
