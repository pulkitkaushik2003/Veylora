package com.example.veylora.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "devices",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_devices_device_uid",
            columnNames = "device_uid"
        )
    }
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_uid", nullable = false, length = 150)
    private String deviceUid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 50)
    private String platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.REGISTERED;

    private LocalDateTime lastSeenAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DeviceStatus {
        REGISTERED, ONLINE, OFFLINE, DISABLED
    }

    public Device() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceUid() { return deviceUid; }
    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
