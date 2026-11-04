package com.example.veylora.Service;


import com.example.veylora.Model.Device;
import com.example.veylora.Model.SimCard;
import com.example.veylora.Repository.SimCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SimCardService {

    private final SimCardRepository simCardRepository;
    private final DeviceService deviceService;

    public SimCardService(SimCardRepository simCardRepository,
                          DeviceService deviceService) {
        this.simCardRepository = simCardRepository;
        this.deviceService = deviceService;
    }

    @Transactional
    public SimCard addSimCard(Long userId, Long deviceId,
                              String label, String phoneNumber,
                              String carrier, Integer slotIndex) {

        Device device = deviceService.getDevice(userId, deviceId);

        if (device.getStatus() == Device.DeviceStatus.DISABLED) {
            throw new IllegalStateException("Cannot add a SIM to a disabled device");
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (slotIndex != null && slotIndex < 0) {
            throw new IllegalArgumentException("SIM slot index cannot be negative");
        }

        SimCard simCard = new SimCard();
        simCard.setLabel(label);
        simCard.setPhoneNumber(phoneNumber);
        simCard.setCarrier(carrier);
        simCard.setSlotIndex(slotIndex);
        simCard.setStatus(SimCard.SimStatus.ACTIVE);
        simCard.setDevice(device);

        return simCardRepository.save(simCard);
    }

    @Transactional(readOnly = true)
    public List<SimCard> getDeviceSimCards(Long userId, Long deviceId) {
        // Confirms that this device belongs to the requesting user.
        deviceService.getDevice(userId, deviceId);
        return simCardRepository.findByDevice_Id(deviceId);
    }

    @Transactional(readOnly = true)
    public SimCard getSimCard(Long userId, Long deviceId, Long simCardId) {
        // First verify device ownership.
        deviceService.getDevice(userId, deviceId);

        return simCardRepository.findByIdAndDevice_Id(simCardId, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("SIM card not found"));
    }

    @Transactional
    public SimCard updateSimStatus(Long userId, Long deviceId,
                                   Long simCardId, SimCard.SimStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("SIM status is required");
        }

        SimCard simCard = getSimCard(userId, deviceId, simCardId);
        simCard.setStatus(status);

        return simCardRepository.save(simCard);
    }

    @Transactional
    public SimCard updateSimCard(Long userId, Long deviceId, Long simCardId,
                                 String label, String phoneNumber,
                                 String carrier, Integer slotIndex,
                                 Integer dailyCallLimit) {
        SimCard simCard = getSimCard(userId, deviceId, simCardId);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        if (slotIndex != null && slotIndex < 0) {
            throw new IllegalArgumentException("SIM slot index cannot be negative");
        }
        if (dailyCallLimit != null && dailyCallLimit < 0) {
            throw new IllegalArgumentException("Daily call limit cannot be negative");
        }

        simCard.setLabel(label);
        simCard.setPhoneNumber(phoneNumber);
        simCard.setCarrier(carrier);
        simCard.setSlotIndex(slotIndex);
        simCard.setDailyCallLimit(dailyCallLimit);

        return simCardRepository.save(simCard);
    }

    @Transactional
    public void deleteSimCard(Long userId, Long deviceId, Long simCardId) {
        SimCard simCard = getSimCard(userId, deviceId, simCardId);
        simCardRepository.delete(simCard);
    }
}