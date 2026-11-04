package com.example.veylora.Controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.veylora.Model.Device;
import com.example.veylora.Service.DeviceService;
import com.example.veylora.config.CurrentUserId;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    //     // Register a device
    // @PostMapping
    // public ResponseEntity<DeviceResponse> registerDevice(
    //         @CurrentUserId Long userId,
    //         @RequestBody DeviceRequest request) {

    //     Device device = deviceService.registerDevice(
    //         userId,
    //         request.deviceUid(),
    //         request.name(),
    //         request.model(),
    //         request.manufacturer(),
    //         request.platform()
    //     );

    //     return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(device));
    // }

    // Get all devices for a user
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @CurrentUserId  Long userId) {

        return ResponseEntity.ok(
            deviceService.getUserDevices(userId).stream()
                .map(DeviceController::toResponse)
                .toList()
        );
    }

    // Get one device
    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceResponse> getDevice(
            @CurrentUserId  Long userId,
            @PathVariable Long deviceId) {

        return ResponseEntity.ok(toResponse(
                deviceService.getDevice(userId, deviceId)));
    }

    // Update device status
    @PatchMapping("/{deviceId}/status")
    public ResponseEntity<DeviceResponse> updateStatus(
            @CurrentUserId  Long userId,
            @PathVariable Long deviceId,
            @RequestBody StatusRequest request) {

        return ResponseEntity.ok(toResponse(
            deviceService.updateDeviceStatus(
                userId, deviceId, request.status()
            )));
    }

    // // Update last-seen time after a device heartbeat
    // @PostMapping("/{deviceId}/heartbeat")
    // public ResponseEntity<DeviceResponse> heartbeat(
    //         @CurrentUserId  Long userId,
    //         @PathVariable Long deviceId) {

    //     return ResponseEntity.ok(toResponse(
    //         deviceService.updateLastSeen(userId, deviceId)));
    // }

    // Disable a device
    @PatchMapping("/{deviceId}/disable")
    public ResponseEntity<DeviceResponse> disableDevice(
            @CurrentUserId  Long userId,
            @PathVariable Long deviceId) {

        return ResponseEntity.ok(toResponse(
            deviceService.disableDevice(userId, deviceId)));
    }

    // Delete a device record entirely for the user
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteDevice(
            @CurrentUserId  Long userId,
            @PathVariable Long deviceId) {

        deviceService.deleteDevice(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    // public record DeviceRequest(
    //     String deviceUid,
    //     String name,
    //     String model,
    //     String manufacturer,
    //     String platform
    // ) {}

    public record StatusRequest(Device.DeviceStatus status) {}

    private static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceUid(),
                device.getName(),
                device.getModel(),
                device.getManufacturer(),
                device.getPlatform(),
                device.getStatus(),
                device.getLastSeenAt(),
                device.getCreatedAt());
    }

    public record DeviceResponse(
            Long id,
            String deviceUid,
            String name,
            String model,
            String manufacturer,
            String platform,
            Device.DeviceStatus status,
            LocalDateTime lastSeenAt,
            LocalDateTime createdAt) {}
}
