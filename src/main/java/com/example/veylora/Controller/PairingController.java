package com.example.veylora.Controller;

import com.example.veylora.Model.Device;
import com.example.veylora.Service.PairingService;
import com.example.veylora.config.CurrentUserId;
import com.example.veylora.config.PairingRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/devices")
public class PairingController {

    private final PairingService pairingService;
    private final PairingRateLimiter rateLimiter;

    public PairingController(PairingService pairingService, PairingRateLimiter rateLimiter) {
        this.pairingService = pairingService;
        this.rateLimiter = rateLimiter;
    }

    /** Logged-in user asks for a pairing code. */
    @PostMapping("/pairing-codes")
    public ResponseEntity<CodeResponse> createCode(@CurrentUserId Long userId) {
        PairingService.CreatedCode created = pairingService.createCode(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CodeResponse(created.code(), created.expiresAt()));
    }

    /** The phone redeems the code. Public route (see SecurityConfig), rate-limited. */
    @PostMapping("/pair")
    public ResponseEntity<PairResponse> pair(@Valid @RequestBody PairRequest request,
                                             HttpServletRequest http) {
        rateLimiter.check(http.getRemoteAddr());
        PairingService.PairResult result = pairingService.pair(
                request.code(), request.deviceUid(), request.name(),
                request.model(), request.manufacturer(), request.platform());
        Device device = result.device();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PairResponse(device.getId(), result.deviceToken()));
    }

    public record PairRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 150) String deviceUid,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 100) String model,
            @Size(max = 100) String manufacturer,
            @Size(max = 50) String platform) {}

    public record CodeResponse(String code, Instant expiresAt) {}

    public record PairResponse(Long deviceId, String deviceToken) {}
}