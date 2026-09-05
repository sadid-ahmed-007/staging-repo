package com.eduauth.controller.university;

import com.eduauth.dto.dashboard.UniversityDashboardDto;
import com.eduauth.model.User;
import com.eduauth.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/university/dashboard")
@RequiredArgsConstructor
public class UniversityDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('UNIVERSITY')")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal User user) {
        UniversityDashboardDto stats = dashboardService.getUniversityDashboardStats(user);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}
