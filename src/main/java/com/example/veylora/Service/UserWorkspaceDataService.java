package com.example.veylora.Service;

import com.example.veylora.Model.User;
import com.example.veylora.Model.UserWorkspaceData;
import com.example.veylora.Repository.UserRepository;
import com.example.veylora.Repository.UserWorkspaceDataRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class UserWorkspaceDataService {

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "ai-agents",
            "templates",
            "automations",
            "notifications",
            "audit-logs",
            "dlt-settings",
            "telephony-settings",
            "user-preferences");

    private final UserWorkspaceDataRepository workspaceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UserWorkspaceDataService(
            UserWorkspaceDataRepository workspaceRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, JsonNode> getAll(Long userId) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (UserWorkspaceData item : workspaceRepository.findByUser_Id(userId)) {
            try {
                result.put(item.getDataKey(), objectMapper.readTree(item.getPayload()));
            } catch (JacksonException exception) {
                throw new IllegalStateException(
                        "Stored workspace data is invalid for key " + item.getDataKey(),
                        exception);
            }
        }
        return result;
    }

    @Transactional
    public JsonNode save(Long userId, String dataKey, JsonNode payload) {
        if (!ALLOWED_KEYS.contains(dataKey)) {
            throw new IllegalArgumentException("Unsupported workspace data key");
        }
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("Workspace data cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String serializedPayload = writePayload(payload);
        if (serializedPayload.length() > 1_000_000) {
            throw new IllegalArgumentException("Workspace data exceeds the 1 MB limit");
        }
        UserWorkspaceData item = workspaceRepository.findByUser_IdAndDataKey(userId, dataKey)
                .orElseGet(UserWorkspaceData::new);
        item.setDataKey(dataKey);
        item.setPayload(serializedPayload);
        item.setUser(user);
        workspaceRepository.save(item);
        return payload;
    }

    private String writePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Workspace data could not be serialized", exception);
        }
    }
}
