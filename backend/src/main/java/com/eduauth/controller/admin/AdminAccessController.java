package com.eduauth.controller.admin;

import com.eduauth.dto.access.AccessGrantListDto;
import com.eduauth.model.AccessGrant;
import com.eduauth.model.User;
import com.eduauth.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin endpoints for system-wide access grant monitoring and revocation.
 *
 * GET    /api/admin/access-grants       → list grants with filters
 * DELETE /api/admin/access-grants/{id}  → admin revokes grant
 */
@RestController
@RequestMapping("/api/admin/access-grants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccessController {

    private final AccessService accessService;

    @GetMapping
    public ResponseEntity<?> getAccessGrants(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long verifierId,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("grantedAt").descending());
        Page<AccessGrant> pageResult = accessService.getAdminGrants(studentId, verifierId, status, pageable);

        LocalDateTime now = LocalDateTime.now();
        List<AccessGrantListDto> dtos = pageResult.getContent().stream()
                .map(g -> accessService.toGrantDto(g, now))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    dtos,
                "total",   pageResult.getTotalElements(),
                "page",    pageResult.getNumber(),
                "pages",   pageResult.getTotalPages()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeAccessGrant(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        accessService.adminRevokeGrant(id, user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Access grant revoked successfully"
        ));
    }
}
