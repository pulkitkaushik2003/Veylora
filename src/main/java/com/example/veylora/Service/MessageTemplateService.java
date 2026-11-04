package com.example.veylora.Service;

import com.example.veylora.Exception.ResourceNotFoundException;
import com.example.veylora.Model.MessageTemplate;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.MessageTemplateRepository;
import com.example.veylora.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageTemplateService {

    private final MessageTemplateRepository repository;
    private final UserRepository userRepository;

    public MessageTemplateService(MessageTemplateRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageTemplate create(Long userId, String name, MessageTemplate.Channel channel,
                                  String subject, String body) {
        MessageTemplate template = new MessageTemplate();
        template.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
        apply(template, name, channel, subject, body);
        return repository.save(template);
    }

    @Transactional
    public MessageTemplate update(Long userId, Long id, String name, MessageTemplate.Channel channel,
                                  String subject, String body) {
        MessageTemplate template = get(userId, id);
        apply(template, name, channel, subject, body);
        return repository.save(template);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> list(Long userId) {
        return repository.findByUser_IdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public MessageTemplate get(Long userId, Long id) {
        return repository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(get(userId, id));
    }

    private static void apply(MessageTemplate t, String name, MessageTemplate.Channel channel,
                              String subject, String body) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Template name is required");
        if (name.length() > 100) throw new IllegalArgumentException("Template name is too long (max 100)");
        if (channel == null) throw new IllegalArgumentException("Template channel (SMS or EMAIL) is required");
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Template text is required");
        if (body.length() > 5000) throw new IllegalArgumentException("Template text is too long (max 5000)");
        if (channel == MessageTemplate.Channel.EMAIL) {
            if (subject == null || subject.isBlank()) throw new IllegalArgumentException("An email template needs a subject");
            if (subject.length() > 300) throw new IllegalArgumentException("Subject is too long (max 300)");
        }
        t.setName(name.trim());
        t.setChannel(channel);
        t.setSubject(channel == MessageTemplate.Channel.EMAIL ? subject.trim() : null);
        t.setBody(body);
    }
}
