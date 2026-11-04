package com.example.veylora.Controller;

import com.example.veylora.Model.MessageTemplate;
import com.example.veylora.Service.MessageTemplateService;
import com.example.veylora.config.CurrentUserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
public class MessageTemplateController {

    private final MessageTemplateService service;

    public MessageTemplateController(MessageTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<TemplateResponse> list(@CurrentUserId Long userId) {
        return service.list(userId).stream().map(MessageTemplateController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return toResponse(service.get(userId, id));
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@CurrentUserId Long userId, @RequestBody TemplateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(service.create(userId, r.name(), r.channel(), r.subject(), r.body())));
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@CurrentUserId Long userId, @PathVariable Long id,
                                   @RequestBody TemplateRequest r) {
        return toResponse(service.update(userId, id, r.name(), r.channel(), r.subject(), r.body()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private static TemplateResponse toResponse(MessageTemplate t) {
        return new TemplateResponse(t.getId(), t.getName(), t.getChannel(), t.getSubject(),
                t.getBody(), t.getCreatedAt());
    }

    public record TemplateRequest(String name, MessageTemplate.Channel channel, String subject, String body) {}

    public record TemplateResponse(Long id, String name, MessageTemplate.Channel channel,
                                   String subject, String body, LocalDateTime createdAt) {}
}
