package com.eduauth.controller.student;

import com.eduauth.dto.dashboard.StudentDashboardDto;
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
@RequestMapping("/api/student/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal User user) {
        StudentDashboardDto stats = dashboardService.getStudentDashboardStats(user);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}
