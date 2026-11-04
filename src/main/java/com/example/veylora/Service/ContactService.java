package com.example.veylora.Service;

import com.example.veylora.Model.Contact;
import com.example.veylora.Model.User;
import com.example.veylora.Repository.ContactRepository;
import com.example.veylora.Repository.UserRepository;
import com.example.veylora.dto.ContactImportResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public ContactService(ContactRepository contactRepository,
                          UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Contact createContact(Long userId, Contact request) {
        validate(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Contact contact = new Contact();
        contact.setName(normalizeText(request.getName()));
        contact.setPhone(normalizePhone(request.getPhone()));
        contact.setEmail(normalizeEmail(request.getEmail()));
        contact.setCompany(normalizeText(request.getCompany()));
        contact.setNotes(request.getNotes());
        contact.setUser(user);

        return contactRepository.save(contact);
    }

    @Transactional(readOnly = true)
    public List<Contact> getContacts(Long userId, String query) {
        List<Contact> contacts = contactRepository.findByUser_Id(userId);
        if (query == null || query.isBlank()) {
            return contacts;
        }
        String normalized = query.trim().toLowerCase();
        return contacts.stream()
                .filter(contact ->
                        contains(contact.getName(), normalized)
                                || contains(contact.getPhone(), normalized)
                                || contains(contact.getEmail(), normalized)
                                || contains(contact.getCompany(), normalized))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Contact> getContacts(Long userId) {
        return getContacts(userId, null);
    }

    @Transactional(readOnly = true)
    public Contact getContact(Long userId, Long contactId) {
        return contactRepository.findByIdAndUser_Id(contactId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
    }

    @Transactional
    public Contact updateContact(Long userId, Long contactId, Contact request) {
        validate(request);
        Contact contact = getContact(userId, contactId);

        contact.setName(normalizeText(request.getName()));
        contact.setPhone(normalizePhone(request.getPhone()));
        contact.setEmail(normalizeEmail(request.getEmail()));
        contact.setCompany(normalizeText(request.getCompany()));
        contact.setNotes(request.getNotes());

        return contactRepository.save(contact);
    }

    @Transactional
    public void deleteContact(Long userId, Long contactId) {
        Contact contact = getContact(userId, contactId);
        contactRepository.delete(contact);
    }

    @Transactional(readOnly = true)
    public List<Contact> searchByName(Long userId, String name) {
        return contactRepository
                .findByUser_IdAndNameContainingIgnoreCase(userId, name);
    }

    @Transactional
    public ContactImportResult importCsv(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        List<Contact> existingContacts = contactRepository.findByUser_Id(userId);
        Map<String, Set<String>> normalizedExisting = new HashMap<>();
        for (Contact contact : existingContacts) {
            normalizedExisting.computeIfAbsent("phone", key -> java.util.Collections.newSetFromMap(new java.util.HashMap<>())).add(normalizePhone(contact.getPhone()));
            normalizedExisting.computeIfAbsent("email", key -> java.util.Collections.newSetFromMap(new java.util.HashMap<>())).add(normalizeEmail(contact.getEmail()));
        }

        int importedCount = 0;
        int duplicateCount = 0;
        int skippedCount = 0;
        int rowsProcessed = 0;

        String content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded CSV file", e);
        }

        List<String> lines = splitCsvLines(content);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("CSV file must contain a header row and at least one contact row");
        }

        List<String> headers = parseCsvLine(lines.get(0));
        Map<String, Integer> fieldIndex = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            fieldIndex.put(headers.get(i).trim().toLowerCase(), i);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }
            rowsProcessed++;

            List<String> row = parseCsvLine(line);
            String name = getField(row, fieldIndex, "name");
            String phone = normalizePhone(getField(row, fieldIndex, "phone"));
            String email = normalizeEmail(getField(row, fieldIndex, "email"));
            String company = normalizeText(getField(row, fieldIndex, "company"));
            String notes = getField(row, fieldIndex, "notes");

            if ((name == null || name.isBlank()) && (phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
                skippedCount++;
                continue;
            }

            if (name == null || name.isBlank()) {
                name = "Unnamed Contact";
            }

            if ((phone != null && !phone.isBlank() && isDuplicate(existingContacts, phone, null))
                    || (email != null && !email.isBlank() && isDuplicate(existingContacts, null, email))) {
                duplicateCount++;
                continue;
            }

            Contact contact = new Contact();
            contact.setName(name);
            contact.setPhone(phone);
            contact.setEmail(email);
            contact.setCompany(company);
            contact.setNotes(notes);
            contact.setUser(user);
            contactRepository.save(contact);
            existingContacts.add(contact);
            importedCount++;
        }

        return new ContactImportResult(importedCount, duplicateCount, skippedCount, rowsProcessed);
    }

    private void validate(Contact contact) {
        if (contact.getName() == null || contact.getName().isBlank()) {
            throw new IllegalArgumentException("Contact name is required");
        }
        if (contact.getPhone() != null && !contact.getPhone().isBlank() && contact.getPhone().length() > 30) {
            throw new IllegalArgumentException("Phone number exceeds the allowed length");
        }
        if (contact.getEmail() != null && !contact.getEmail().isBlank() && contact.getEmail().length() > 150) {
            throw new IllegalArgumentException("Email exceeds the allowed length");
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replaceAll("[^0-9+]", "").trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private boolean isDuplicate(List<Contact> contacts, String phone, String email) {
        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = normalizeEmail(email);
        for (Contact contact : contacts) {
            boolean phoneMatch = normalizedPhone != null && normalizedPhone.equals(normalizePhone(contact.getPhone()));
            boolean emailMatch = normalizedEmail != null && normalizedEmail.equals(normalizeEmail(contact.getEmail()));
            if (phoneMatch || emailMatch) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitCsvLines(String content) {
        List<String> rows = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                current.append('\u0000');
            } else if ((ch == '\n' || ch == '\r') && !inQuotes) {
                if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                rows.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            rows.add(current.toString());
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString().replace('\u0000', ',').trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String getField(List<String> row, Map<String, Integer> indexMap, String field) {
        Integer index = indexMap.get(field.toLowerCase());
        if (index == null || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }
}
