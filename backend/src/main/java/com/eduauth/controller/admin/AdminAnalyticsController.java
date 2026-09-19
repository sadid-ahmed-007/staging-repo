package com.eduauth.controller.admin;

import com.eduauth.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping({"", "/"})
    public ResponseEntity<?> getAnalytics(@RequestParam(defaultValue = "30") int days) {
        Map<String, Object> data = analyticsService.getAnalytics(days);
        return ResponseEntity.ok(data);
    }
}
