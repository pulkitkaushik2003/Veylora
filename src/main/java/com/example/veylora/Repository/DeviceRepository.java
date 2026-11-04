package com.example.veylora.Repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByTokenHash(String tokenHash);

    List<Device> findByUser_Id(Long userId);

    Optional<Device> findByIdAndUser_Id(Long id, Long userId);

    Optional<Device> findByDeviceUid(String deviceUid);

    boolean existsByDeviceUid(String deviceUid);

    List<Device> findByUser_IdAndStatus(
        Long userId, Device.DeviceStatus status
    );
}
