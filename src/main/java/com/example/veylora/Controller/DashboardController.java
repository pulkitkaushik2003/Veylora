package com.example.veylora.Controller;

import com.example.veylora.config.CurrentUserId;
import com.example.veylora.Service.DashboardService;
import com.example.veylora.dto.DashboardActivityItem;
import com.example.veylora.dto.DashboardSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(@CurrentUserId Long userId) {
        return dashboardService.getSummary(userId);
    }

    @GetMapping("/activity")
    public List<DashboardActivityItem> getActivity(@CurrentUserId Long userId) {
        return dashboardService.getRecentActivity(userId);
    }

    @GetMapping("/channel-stats")
    public DashboardSummaryResponse getChannelStats(@CurrentUserId Long userId) {
        return dashboardService.getSummary(userId);
    }
}
