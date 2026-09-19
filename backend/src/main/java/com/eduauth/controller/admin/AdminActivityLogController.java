package com.eduauth.controller.admin;

import com.eduauth.model.ActivityLog;
import com.eduauth.model.User;
import com.eduauth.repository.ActivityLogRepository;
import com.eduauth.repository.UserRepository;
import com.eduauth.repository.specification.ActivityLogSpecification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityLogController {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository        userRepository;
    private final ObjectMapper         objectMapper;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getActivityLogs(
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15", name = "per_page", required = false) int perPage) {

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, perPage, Sort.by("createdAt").descending());

        var spec = ActivityLogSpecification.withFilters(filter, type);
        Page<ActivityLog> paged = activityLogRepository.findAll(spec, pageable);

        Map<Long, User> userCache = new HashMap<>();

        List<Map<String, Object>> items = paged.getContent().stream()
                .map(log -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",                 log.getId());
                    m.put("created_at",         log.getCreatedAt());
                    m.put("action",             log.getAction());
                    m.put("description",        log.getDescription());
                    m.put("ip_address",         log.getIpAddress());

                    String userName = "System";
                    String userRole = "system";

                    if (log.getUserId() != null) {
                        User u = userCache.computeIfAbsent(log.getUserId(), id -> userRepository.findById(id).orElse(null));
                        if (u != null) {
                            userRole = u.getRole() != null ? u.getRole() : "user";
                            if ("student".equalsIgnoreCase(userRole) && u.getStudent() != null) {
                                String fname = u.getStudent().getFirstName() != null ? u.getStudent().getFirstName() : "";
                                String lname = u.getStudent().getLastName()  != null ? u.getStudent().getLastName()  : "";
                                String fullName = (fname + " " + lname).trim();
                                userName = !fullName.isEmpty() ? fullName : u.getEmail();
                            } else if ("university".equalsIgnoreCase(userRole) && u.getInstitution() != null) {
                                userName = u.getInstitution().getName() != null ? u.getInstitution().getName() : u.getEmail();
                            } else if ("verifier".equalsIgnoreCase(userRole) && u.getVerifier() != null) {
                                userName = u.getVerifier().getCompanyName() != null ? u.getVerifier().getCompanyName() : u.getEmail();
                            } else {
                                userName = u.getEmail() != null ? u.getEmail() : "User #" + u.getId();
                            }
                        }
                    }

                    m.put("user_name",          userName);
                    m.put("user_role",          userRole);
                    m.put("action_description", log.getDescription() != null ? log.getDescription() : log.getAction().replace('_', ' '));
                    m.put("target",             extractTarget(log));

                    return m;
                })
                .toList();

        Map<String, Object> logsData = new LinkedHashMap<>();
        logsData.put("data",         items);
        logsData.put("current_page", paged.getNumber() + 1);
        logsData.put("last_page",    Math.max(1, paged.getTotalPages()));
        logsData.put("per_page",     perPage);
        logsData.put("total",        paged.getTotalElements());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data",    items);
        response.put("logs",    logsData);

        return ResponseEntity.ok(response);
    }

    private String extractTarget(ActivityLog log) {
        if (log.getMetadata() != null && !log.getMetadata().isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(log.getMetadata());
                if (node.hasNonNull("serial")) {
                    return node.get("serial").asText();
                }
                if (node.hasNonNull("enrollment_number")) {
                    return node.get("enrollment_number").asText();
                }
                if (node.hasNonNull("email")) {
                    return node.get("email").asText();
                }
                if (node.hasNonNull("verifier")) {
                    return node.get("verifier").asText();
                }
                if (node.hasNonNull("institution")) {
                    return node.get("institution").asText();
                }
                if (node.hasNonNull("reason")) {
                    return node.get("reason").asText();
                }
            } catch (Exception ignored) {
            }
        }
        if (log.getEntityType() != null && !log.getEntityType().isBlank()) {
            String entity = log.getEntityType();
            int idx = entity.lastIndexOf('\\');
            if (idx >= 0) {
                entity = entity.substring(idx + 1);
            }
            int dotIdx = entity.lastIndexOf('.');
            if (dotIdx >= 0) {
                entity = entity.substring(dotIdx + 1);
            }
            if (log.getEntityId() != null) {
                return entity + " #" + log.getEntityId();
            }
            return entity;
        }
        return "-";
    }
}
