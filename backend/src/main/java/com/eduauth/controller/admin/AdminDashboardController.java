package com.eduauth.controller.admin;

import com.eduauth.dto.dashboard.AdminDashboardDto;
import com.eduauth.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDashboardStats() {
        AdminDashboardDto stats = dashboardService.getAdminDashboardStats();
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}
