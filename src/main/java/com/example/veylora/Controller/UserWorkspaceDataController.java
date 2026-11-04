package com.example.veylora.Controller;

import com.example.veylora.config.CurrentUserId;
import com.example.veylora.Service.UserWorkspaceDataService;

import tools.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspace-data")
public class UserWorkspaceDataController {

    private final UserWorkspaceDataService workspaceDataService;

    public UserWorkspaceDataController(UserWorkspaceDataService workspaceDataService) {
        this.workspaceDataService = workspaceDataService;
    }

    @GetMapping
    public Map<String, JsonNode> getAll(@CurrentUserId Long userId) {
        return workspaceDataService.getAll(userId);
    }

    @PutMapping("/{key}")
    public JsonNode save(
            @CurrentUserId Long userId,
            @PathVariable String key,
            @RequestBody JsonNode payload) {
        return workspaceDataService.save(userId, key, payload);
    }
}
