package com.example.veylora.Controller;

import com.example.veylora.Repository.PersonalFileRepository;
import com.example.veylora.Service.PersonalFileService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/personal-files")
public class PersonalFileController {

    private final PersonalFileService service;

    public PersonalFileController(PersonalFileService service) {
        this.service = service;
    }

    /** One or more files, or a .zip. Returns how many were added and which were skipped. */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PersonalFileService.UploadResult> upload(@CurrentUserId Long userId,
                                                                   @PathVariable Long campaignId,
                                                                   @RequestParam("files") List<MultipartFile> files)
            throws IOException {
        int added = 0, replaced = 0;
        List<String> skipped = new ArrayList<>();
        for (MultipartFile file : files) {
            PersonalFileService.UploadResult r = service.upload(userId, campaignId, file.getOriginalFilename(), file.getBytes());
            added += r.added();
            replaced += r.replaced();
            skipped.addAll(r.skipped());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PersonalFileService.UploadResult(added, replaced, skipped));
    }

    /** The document list: which email gets which uploaded file name. Replaces any earlier list. */
    @PutMapping("/mapping")
    public PersonalFileService.MapResult saveMapping(@CurrentUserId Long userId, @PathVariable Long campaignId,
                                                     @RequestBody MappingRequest request) {
        return service.saveMapping(userId, campaignId, request.pairs());
    }

    public record MappingRequest(List<PersonalFileService.MapPair> pairs) {}

    @GetMapping
    public List<FileResponse> list(@CurrentUserId Long userId, @PathVariable Long campaignId) {
        return service.list(userId, campaignId).stream()
                .map(i -> new FileResponse(i.getId(), i.getFileName(), i.getSizeBytes())).toList();
    }

    /** Dry run: who has a file, who does not, which files match nobody. */
    @GetMapping("/check")
    public PersonalFileService.MatchReport check(@CurrentUserId Long userId, @PathVariable Long campaignId) {
        return service.check(userId, campaignId);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long campaignId,
                                       @PathVariable Long fileId) {
        service.delete(userId, campaignId, fileId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@CurrentUserId Long userId, @PathVariable Long campaignId) {
        service.deleteAll(userId, campaignId);
        return ResponseEntity.noContent().build();
    }

    public record FileResponse(Long id, String fileName, long sizeBytes) {}
}
