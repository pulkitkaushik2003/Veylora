
package com.example.veylora.Controller;

import com.example.veylora.Model.Contact;
import com.example.veylora.Service.ContactService;
import com.example.veylora.config.CurrentUserId;
import com.example.veylora.dto.ContactImportResult;
import com.example.veylora.dto.ContactRequest;
import com.example.veylora.dto.ContactResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.veylora.config.CurrentUserId;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody ContactRequest request) {

        Contact contact = new Contact();
        contact.setName(request.name());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setCompany(request.company());
        contact.setNotes(request.notes());

        Contact saved = contactService.createContact(userId, contact);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    @GetMapping
    public List<ContactResponse> getAll(
            @CurrentUserId Long userId,
            @RequestParam(required = false) String query) {
        return contactService.getContacts(userId, query)
                .stream()
                .map(ContactController::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ContactResponse getOne(
            @CurrentUserId Long userId,
            @PathVariable Long id) {

        return toResponse(contactService.getContact(userId, id));
    }

    @PutMapping("/{id}")
    public ContactResponse update(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ContactRequest request) {

        Contact contact = new Contact();
        contact.setName(request.name());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setCompany(request.company());
        contact.setNotes(request.notes());

        return toResponse(
                contactService.updateContact(userId, id, contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId Long userId,
            @PathVariable Long id) {

        contactService.deleteContact(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ContactResponse> search(
            @CurrentUserId Long userId,
            @RequestParam String name) {

        return contactService.searchByName(userId, name)
                .stream()
                .map(ContactController::toResponse)
                .toList();
    }

    @PostMapping("/import")
    public ResponseEntity<ContactImportResult> importCsv(
            @CurrentUserId  Long userId,
            @RequestParam("file") MultipartFile file) {
        ContactImportResult result = contactService.importCsv(userId, file);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    private static ContactResponse toResponse(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getCompany(),
                contact.getNotes(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
