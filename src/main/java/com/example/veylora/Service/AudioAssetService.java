package com.example.veylora.Service;

import com.example.veylora.Model.AudioAsset;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.AudioAssetRepository;
import com.example.veylora.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AudioAssetService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final AudioAssetRepository audioRepository;
    private final UserRepository userRepository;
    private final Path storageDirectory;

    public AudioAssetService(
            AudioAssetRepository audioRepository,
            UserRepository userRepository,
            @Value("${app.audio.storage-dir:uploads/audio}") String storageDirectory) {
        this.audioRepository = audioRepository;
        this.userRepository = userRepository;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<AudioAsset> getAudioAssets(Long userId) {
        return audioRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public AudioAsset upload(
            Long userId,
            String title,
            AudioAsset.Language language,
            MultipartFile file) {
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Audio title is required and must be at most 200 characters.");
        }
        if (language == null) {
            throw new IllegalArgumentException("Audio language is required.");
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Choose a non-empty MP3 or WAV file up to 10 MB.");
        }

        String originalFilename = Path.of(file.getOriginalFilename() == null
                ? "audio" : file.getOriginalFilename()).getFileName().toString();
        String extension = extensionOf(originalFilename);
        validateAudioContent(file, extension);
        User user = findUser(userId);

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path storedPath = storageDirectory.resolve(storedFilename).normalize();
        if (!storedPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid audio filename.");
        }

        try {
            Files.createDirectories(storageDirectory);
            try (var input = file.getInputStream()) {
                Files.copy(input, storedPath);
            }
            AudioAsset asset = new AudioAsset();
            asset.setTitle(title.trim());
            asset.setFilename(originalFilename);
            asset.setStoragePath(storedPath.toString());
            asset.setDurationSeconds(0);
            asset.setFileSizeBytes(file.getSize());
            asset.setLanguage(language);
            asset.setType(AudioAsset.AssetType.UPLOADED_AUDIO);
            asset.setVoiceName("Uploaded Audio");
            asset.setVersion(1);
            asset.setActive(true);
            asset.setUser(user);

            try {
                return audioRepository.save(asset);
            } catch (RuntimeException exception) {
                try {
                    Files.deleteIfExists(storedPath);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
                throw exception;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store the uploaded audio file.", exception);
        }
    }

    @Transactional
    public AudioAsset createTts(
            Long userId,
            String title,
            String script,
            AudioAsset.Language language,
            String voiceName,
            Integer durationSeconds,
            Long fileSizeBytes) {
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Audio title is required and must be at most 200 characters.");
        }
        if (script == null || script.isBlank() || script.length() > 10000) {
            throw new IllegalArgumentException("TTS script is required and must be at most 10,000 characters.");
        }
        if (language == null) {
            throw new IllegalArgumentException("Audio language is required.");
        }

        AudioAsset asset = new AudioAsset();
        asset.setTitle(title.trim());
        asset.setFilename(title.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_") + "_v1.mp3");
        asset.setStoragePath("");
        asset.setDurationSeconds(Math.max(0, durationSeconds == null ? 0 : durationSeconds));
        asset.setFileSizeBytes(Math.max(0, fileSizeBytes == null ? 0 : fileSizeBytes));
        asset.setLanguage(language);
        asset.setType(AudioAsset.AssetType.TTS_GENERATED);
        asset.setTtsScript(script);
        asset.setVoiceName(voiceName);
        asset.setVersion(1);
        asset.setActive(true);
        asset.setUser(findUser(userId));
        return audioRepository.save(asset);
    }

    @Transactional
    public AudioAsset setActive(Long userId, Long audioId, boolean active) {
        AudioAsset asset = getAudioAsset(userId, audioId);
        asset.setActive(active);
        return audioRepository.save(asset);
    }

    @Transactional
    public void delete(Long userId, Long audioId) {
        AudioAsset asset = getAudioAsset(userId, audioId);
        audioRepository.delete(asset);
        if (!asset.getStoragePath().isBlank()) {
            try {
                Files.deleteIfExists(resolveStoredPath(asset.getStoragePath()));
            } catch (IOException exception) {
                throw new IllegalStateException("Audio metadata was deleted but its file could not be removed.", exception);
            }
        }
    }

    @Transactional(readOnly = true)
    public StoredAudio getStoredAudio(Long userId, Long audioId) {
        AudioAsset asset = getAudioAsset(userId, audioId);
        if (asset.getType() != AudioAsset.AssetType.UPLOADED_AUDIO
                || asset.getStoragePath().isBlank()) {
            throw new IllegalArgumentException("This audio asset has no uploaded audio file.");
        }

        Path path = resolveStoredPath(asset.getStoragePath());
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("The stored audio file is missing from the server.");
        }
        String contentType = path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav")
                ? "audio/wav" : "audio/mpeg";
        return new StoredAudio(path, contentType, asset.getFilename());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private AudioAsset getAudioAsset(Long userId, Long audioId) {
        return audioRepository.findByIdAndUser_Id(audioId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Audio asset not found."));
    }

    private Path resolveStoredPath(String storedPath) {
        Path path = Path.of(storedPath).toAbsolutePath().normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalStateException("Stored audio path is outside the configured storage directory.");
        }
        return path;
    }

    private static String extensionOf(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mp3")) return "mp3";
        if (lower.endsWith(".wav")) return "wav";
        throw new IllegalArgumentException("Only MP3 and WAV audio files are supported.");
    }

    private static void validateAudioContent(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                && !(extension.equals("mp3") && contentType.equals("audio/mpeg"))
                && !(extension.equals("wav")
                        && (contentType.equals("audio/wav") || contentType.equals("audio/x-wav")))) {
            throw new IllegalArgumentException("The selected file type does not match its extension.");
        }

        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = extension.equals("wav")
                    ? header.length >= 12
                            && header[0] == 'R' && header[1] == 'I'
                            && header[2] == 'F' && header[3] == 'F'
                            && header[8] == 'W' && header[9] == 'A'
                            && header[10] == 'V' && header[11] == 'E'
                    : header.length >= 3
                            && ((header[0] == 'I' && header[1] == 'D' && header[2] == '3')
                                    || ((header[0] & 0xFF) == 0xFF
                                            && ((header[1] & 0xE0) == 0xE0)));
            if (!valid) {
                throw new IllegalArgumentException("The selected file is not a valid MP3 or WAV file.");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read the selected audio file.", exception);
        }
    }

    public record StoredAudio(Path path, String contentType, String filename) {
    }
}
