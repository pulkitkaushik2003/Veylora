package com.example.veylora.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sim_cards")
public class SimCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String label;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 30)
    private String carrier;

    // Slot index reported by the device, if available.
    private Integer slotIndex;

    private Integer dailyCallLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SimStatus status = SimStatus.UNKNOWN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SimStatus {
        ACTIVE, INACTIVE, UNKNOWN
    }

    public SimCard() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public Integer getSlotIndex() { return slotIndex; }
    public void setSlotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
    }

    public Integer getDailyCallLimit() { return dailyCallLimit; }
    public void setDailyCallLimit(Integer dailyCallLimit) {
        this.dailyCallLimit = dailyCallLimit;
    }

    public SimStatus getStatus() { return status; }
    public void setStatus(SimStatus status) {
        this.status = status;
    }

    public Device getDevice() { return device; }
    public void setDevice(Device device) {
        this.device = device;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
