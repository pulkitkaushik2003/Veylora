package com.example.veylora.Service;

import com.example.veylora.Model.Device;
import com.example.veylora.Model.PairingCode;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.DeviceRepository;
import com.example.veylora.Repository.PairingCodeRepository;
import com.example.veylora.Repository.UserRepository;
import com.example.veylora.util.Hashing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class PairingService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int CODE_LENGTH = 8;
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String INVALID = "Invalid or expired pairing code";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PairingCodeRepository codeRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public PairingService(PairingCodeRepository codeRepository,
                          DeviceRepository deviceRepository,
                          UserRepository userRepository) {
        this.codeRepository = codeRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    public record CreatedCode(String code, Instant expiresAt) {}
    public record PairResult(Device device, String deviceToken) {}

    @Transactional
    public CreatedCode createCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String code = randomCode();
        PairingCode entity = new PairingCode();
        entity.setCodeHash(Hashing.sha256(code));
        entity.setUser(user);
        entity.setExpiresAt(Instant.now().plus(CODE_TTL));
        codeRepository.save(entity);

        return new CreatedCode(code, entity.getExpiresAt());
    }

    @Transactional
    public PairResult pair(String rawCode, String deviceUid, String name,
                           String model, String manufacturer, String platform) {

        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        PairingCode pairing = codeRepository.findByCodeHash(Hashing.sha256(code))
                .orElseThrow(() -> new IllegalArgumentException(INVALID));

        if (pairing.getUsedAt() != null || pairing.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException(INVALID);
        }
        if (deviceRepository.existsByDeviceUid(deviceUid)) {
            throw new IllegalStateException("This device is already registered");
        }

        pairing.setUsedAt(Instant.now()); // single use

        String token = randomToken();

        Device device = new Device();
        device.setDeviceUid(deviceUid);
        device.setName(name);
        device.setModel(model);
        device.setManufacturer(manufacturer);
        device.setPlatform(platform);
        device.setStatus(Device.DeviceStatus.REGISTERED);
        device.setUser(pairing.getUser());          // owner comes from the CODE, never from the request
        device.setTokenHash(Hashing.sha256(token));

        return new PairResult(deviceRepository.save(device), token);
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String randomToken() {
        byte[] bytes = new byte[32]; // 256 bits
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}