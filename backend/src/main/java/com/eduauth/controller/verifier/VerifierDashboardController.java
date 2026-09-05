package com.eduauth.controller.verifier;

import com.eduauth.dto.dashboard.VerifierDashboardDto;
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
@RequestMapping("/api/verifier/dashboard")
@RequiredArgsConstructor
public class VerifierDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('VERIFIER')")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal User user) {
        VerifierDashboardDto stats = dashboardService.getVerifierDashboardStats(user);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}
