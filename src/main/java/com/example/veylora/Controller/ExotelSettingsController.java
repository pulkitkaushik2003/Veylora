package com.example.veylora.Controller;

import com.example.veylora.Service.UserExotelSettingsService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** The logged-in user's own Exotel account. The API token is write-only: it is never sent back. */
@RestController
@RequestMapping("/api/v1/exotel-settings")
public class ExotelSettingsController {

    private final UserExotelSettingsService service;

    public ExotelSettingsController(UserExotelSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public UserExotelSettingsService.View get(@CurrentUserId Long userId) {
        return service.get(userId);
    }

    @PutMapping
    public UserExotelSettingsService.View save(@CurrentUserId Long userId, @RequestBody UserExotelSettingsService.SaveRequest r) {
        return service.save(userId, r);
    }

    @DeleteMapping
    public UserExotelSettingsService.View remove(@CurrentUserId Long userId) {
        return service.remove(userId);
    }

    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, String>> testSms(@CurrentUserId Long userId, @RequestBody TestSms r) {
        service.testSms(userId, r.to(), r.body(), r.dltTemplateId());
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PostMapping("/test-call")
    public ResponseEntity<Map<String, String>> testCall(@CurrentUserId Long userId, @RequestBody TestCall r) {
        service.testCall(userId, r.to(), r.appId());
        return ResponseEntity.ok(Map.of("status", "calling"));
    }

    public record TestSms(String to, String body, String dltTemplateId) {}

    public record TestCall(String to, String appId) {}
}
