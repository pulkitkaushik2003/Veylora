package com.example.veylora.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.veylora.Exception.ResourceNotFoundException;
import com.example.veylora.Model.Device;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.DeviceRepository;
import com.example.veylora.Repository.SimCardRepository;
import com.example.veylora.Repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final SimCardRepository simCardRepository;

    public DeviceService(
            DeviceRepository deviceRepository,
            UserRepository userRepository,
            SimCardRepository simCardRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.simCardRepository = simCardRepository;
    }

// needs: import com.example.veylora.Exception.ResourceNotFoundException;
@Transactional
public Device heartbeatByDeviceId(Long deviceId) {
    Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    if (device.getStatus() == Device.DeviceStatus.DISABLED) {
        throw new IllegalStateException("Device is disabled");
    }
    device.setLastSeenAt(LocalDateTime.now());
    device.setStatus(Device.DeviceStatus.ONLINE);
    return deviceRepository.save(device);
}


    // // Register a new device for a user
    // @Transactional
    // public Device registerDevice(
    //         Long userId,
    //         String deviceUid,
    //         String name,
    //         String model,
    //         String manufacturer,
    //         String platform) {

    //     if (deviceUid == null || deviceUid.isBlank()) {
    //         throw new IllegalArgumentException(
    //             "Device ID is required"
    //         );
    //     }

    //     if (name == null || name.isBlank()) {
    //         throw new IllegalArgumentException(
    //             "Device name is required"
    //         );
    //     }

    //     if (deviceRepository.existsByDeviceUid(deviceUid)) {
    //         throw new IllegalArgumentException(
    //             "Device is already registered"
    //         );
    //     }

    //     User user = userRepository.findById(userId)
    //         .orElseThrow(() ->
    //             new IllegalArgumentException("User not found")
    //         );

    //     Device device = new Device();
    //     device.setDeviceUid(deviceUid);
    //     device.setName(name);
    //     device.setModel(model);
    //     device.setManufacturer(manufacturer);
    //     device.setPlatform(platform);
    //     device.setStatus(Device.DeviceStatus.REGISTERED);
    //     device.setUser(user);

    //     return deviceRepository.save(device);
    // }

    // Get all devices belonging to a user
    @Transactional(readOnly = true)
    public List<Device> getUserDevices(Long userId) {
        return deviceRepository.findByUser_Id(userId);
    }

    // Get one device only if it belongs to this user
    @Transactional(readOnly = true)
    public Device getDevice(Long userId, Long deviceId) {
        return deviceRepository
            .findByIdAndUser_Id(deviceId, userId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Device not found for this user"
                )
            );
    }

    // Update device connection status
    @Transactional
    public Device updateDeviceStatus(
            Long userId,
            Long deviceId,
            Device.DeviceStatus status) {

        if (status == null) {
            throw new IllegalArgumentException(
                "Device status is required"
            );
        }

        Device device = getDevice(userId, deviceId);
        device.setStatus(status);

        if (status == Device.DeviceStatus.ONLINE) {
            device.setLastSeenAt(LocalDateTime.now());
        }

        return deviceRepository.save(device);
    }

    // Record when a device last contacted the backend
    @Transactional
    public Device updateLastSeen(Long userId, Long deviceId) {
        Device device = getDevice(userId, deviceId);

        device.setLastSeenAt(LocalDateTime.now());
        device.setStatus(Device.DeviceStatus.ONLINE);

        return deviceRepository.save(device);
    }

    // Disable a device without deleting its history
    @Transactional
    public Device disableDevice(Long userId, Long deviceId) {
        Device device = getDevice(userId, deviceId);
        device.setStatus(Device.DeviceStatus.DISABLED);

        return deviceRepository.save(device);
    }

    // Remove a device and all associated SIM records for a user
    @Transactional
    public void deleteDevice(Long userId, Long deviceId) {
        Device device = getDevice(userId, deviceId);
        simCardRepository.findByDevice_Id(deviceId).forEach(simCardRepository::delete);
        deviceRepository.delete(device);
    }
}
