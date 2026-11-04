package com.example.veylora.Controller;


import com.example.veylora.Model.SimCard;
import com.example.veylora.Service.SimCardService;
import com.example.veylora.config.CurrentUserId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/sim-cards")
public class SimCardController {

    private final SimCardService simCardService;

    public SimCardController(SimCardService simCardService) {
        this.simCardService = simCardService;
    }

    @PostMapping
    public ResponseEntity<SimCardResponse> create(
            @CurrentUserId Long userId,
            @PathVariable Long deviceId,
            @RequestBody CreateSimRequest request) {

        SimCard sim = simCardService.addSimCard(
                userId, deviceId, request.label(),
                request.phoneNumber(), request.carrier(),
                request.slotIndex());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(sim));
    }

    @GetMapping
    public List<SimCardResponse> getAll(
            @RequestParam Long userId,
            @PathVariable Long deviceId) {

        return simCardService.getDeviceSimCards(userId, deviceId)
                .stream().map(SimCardController::toResponse).toList();
    }

    @GetMapping("/{simCardId}")
    public SimCardResponse getOne(
            @RequestParam Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long simCardId) {

        return toResponse(
                simCardService.getSimCard(userId, deviceId, simCardId));
    }

    @PutMapping("/{simCardId}")
    public SimCardResponse update(
            @RequestParam Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long simCardId,
            @RequestBody UpdateSimRequest request) {

        return toResponse(simCardService.updateSimCard(
                userId, deviceId, simCardId,
                request.label(), request.phoneNumber(),
                request.carrier(), request.slotIndex(), request.dailyCallLimit()));
    }

    @PatchMapping("/{simCardId}/status")
    public SimCardResponse updateStatus(
            @RequestParam Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long simCardId,
            @RequestBody UpdateSimStatusRequest request) {

        return toResponse(simCardService.updateSimStatus(
                userId, deviceId, simCardId, request.status()));
    }

    @DeleteMapping("/{simCardId}")
    public ResponseEntity<Void> delete(
            @RequestParam Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long simCardId) {

        simCardService.deleteSimCard(userId, deviceId, simCardId);
        return ResponseEntity.noContent().build();
    }

    private static SimCardResponse toResponse(SimCard sim) {
        return new SimCardResponse(
                sim.getId(), sim.getLabel(), sim.getPhoneNumber(),
                sim.getCarrier(), sim.getSlotIndex(),
                sim.getStatus(), sim.getDailyCallLimit(), sim.getCreatedAt());
    }

    public record CreateSimRequest(
            String label,
            String phoneNumber,
            String carrier,
            Integer slotIndex,
            Integer dailyCallLimit) {}

    public record UpdateSimRequest(
            String label,
            String phoneNumber,
            String carrier,
            Integer slotIndex,
            Integer dailyCallLimit) {}

    public record UpdateSimStatusRequest(
            SimCard.SimStatus status) {}

    public record SimCardResponse(
            Long id,
            String label,
            String phoneNumber,
            String carrier,
            Integer slotIndex,
            SimCard.SimStatus status,
            Integer dailyCallLimit,
            LocalDateTime createdAt) {}
}