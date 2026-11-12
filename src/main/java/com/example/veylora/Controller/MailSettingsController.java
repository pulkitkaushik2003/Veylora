package com.example.veylora.Controller;

import com.example.veylora.Service.UserMailSettingsService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** The logged-in user's own sending mailbox. The password is write-only: it is never sent back. */
@RestController
@RequestMapping("/api/v1/mail-settings")
public class MailSettingsController {

    private final UserMailSettingsService service;

    public MailSettingsController(UserMailSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public UserMailSettingsService.View get(@CurrentUserId Long userId) {
        return service.get(userId);
    }

    @PutMapping
    public UserMailSettingsService.View save(@CurrentUserId Long userId, @RequestBody SaveRequest r) {
        return service.save(userId, r.email(), r.password(), r.host(), r.port());
    }

    @DeleteMapping
    public UserMailSettingsService.View remove(@CurrentUserId Long userId) {
        return service.remove(userId);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> test(@CurrentUserId Long userId, @RequestBody TestRequest r) {
        service.sendTest(userId, r.to());
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    public record SaveRequest(String email, String password, String host, Integer port) {}

    public record TestRequest(String to) {}
}
