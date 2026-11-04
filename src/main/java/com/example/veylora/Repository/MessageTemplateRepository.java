package com.example.veylora.Repository;

import com.example.veylora.Model.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    List<MessageTemplate> findByUser_IdOrderByIdDesc(Long userId);

    Optional<MessageTemplate> findByIdAndUser_Id(Long id, Long userId);
}
