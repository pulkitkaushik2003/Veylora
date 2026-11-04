package com.example.veylora.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.veylora.Model.Contact;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByUser_Id(Long userId);

    Optional<Contact> findByIdAndUser_Id(Long id, Long userId);

    List<Contact> findByUser_IdAndNameContainingIgnoreCase(
        Long userId, String name
    );

    List<Contact> findByUser_IdAndPhoneContaining(
        Long userId, String phone
    );
}
