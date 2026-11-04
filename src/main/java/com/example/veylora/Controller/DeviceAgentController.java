package com.example.veylora.Controller;

import com.example.veylora.Model.Device;
import com.example.veylora.Service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoints the PHONE calls with its device token. It can only touch its own device. */
@RestController
@RequestMapping("/api/v1/device-agent")
public class DeviceAgentController {

    private final DeviceService deviceService;

    public DeviceAgentController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@AuthenticationPrincipal Long deviceId) {
        Device device = deviceService.heartbeatByDeviceId(deviceId);
        return ResponseEntity.ok(Map.of(
                "deviceId", device.getId(),
                "status", device.getStatus().name(),
                "lastSeenAt", device.getLastSeenAt().toString()));
    }
}