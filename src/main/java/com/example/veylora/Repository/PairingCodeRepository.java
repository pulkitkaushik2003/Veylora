package com.example.veylora.Repository;

import com.example.veylora.Model.PairingCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PairingCodeRepository extends JpaRepository<PairingCode, Long> {

    // Row lock: two simultaneous attempts can't both use the same code.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PairingCode> findByCodeHash(String codeHash);
}