package com.example.veylora.Controller;

import com.example.veylora.Model.AudioAsset;
import com.example.veylora.Service.AudioAssetService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.veylora.config.CurrentUserId;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/audio")
public class AudioAssetController {

    private final AudioAssetService audioAssetService;

    public AudioAssetController(AudioAssetService audioAssetService) {
        this.audioAssetService = audioAssetService;
    }

    @GetMapping
    public List<AudioResponse> getAll(@CurrentUserId Long userId) {
        return audioAssetService.getAudioAssets(userId).stream()
                .map(AudioAssetController::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioResponse> upload(
            @CurrentUserId Long userId,
            @RequestParam @NotBlank @Size(max = 200) String title,
            @RequestParam String language,
            @RequestPart("file") MultipartFile file) {
        AudioAsset asset = audioAssetService.upload(
                userId, title, toLanguage(language), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(asset));
    }

    @PostMapping("/tts")
    public ResponseEntity<AudioResponse> createTts(
            @CurrentUserId Long userId,
            @RequestBody TtsRequest request) {
        AudioAsset asset = audioAssetService.createTts(
                userId,
                request.title(),
                request.ttsScript(),
                toLanguage(request.language()),
                request.voiceName(),
                request.durationSeconds(),
                request.fileSizeBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(asset));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        AudioAssetService.StoredAudio stored = audioAssetService.getStoredAudio(userId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(stored.filename()).build().toString())
                .body(new FileSystemResource(stored.path()));
    }

    @PatchMapping("/{id}/active")
    public AudioResponse setActive(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @RequestBody ActiveRequest request) {
        return toResponse(audioAssetService.setActive(userId, id, request.active()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        audioAssetService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private static AudioAsset.Language toLanguage(String language) {
        try {
            return AudioAsset.Language.valueOf(
                    language.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Supported audio languages are hi-IN, en-IN, and en-US.");
        }
    }

    private static AudioResponse toResponse(AudioAsset asset) {
        return new AudioResponse(
                asset.getId(),
                asset.getTitle(),
                asset.getFilename(),
                asset.getDurationSeconds(),
                asset.getFileSizeBytes(),
                asset.getLanguage().name().replace('_', '-'),
                asset.getType().name(),
                asset.getTtsScript(),
                asset.getVoiceName(),
                asset.getVersion(),
                asset.isActive(),
                asset.getCreatedAt(),
                asset.getType() == AudioAsset.AssetType.UPLOADED_AUDIO
                        ? "/api/v1/audio/" + asset.getId() + "/file"
                        : "");
    }

    public record TtsRequest(
            String title,
            String language,
            String ttsScript,
            String voiceName,
            Integer durationSeconds,
            Long fileSizeBytes) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record AudioResponse(
            Long id,
            String title,
            String filename,
            Integer durationSeconds,
            Long fileSizeBytes,
            String language,
            String type,
            String ttsScript,
            String voiceName,
            Integer version,
            boolean active,
            LocalDateTime createdAt,
            String audioUrl) {
    }
}
