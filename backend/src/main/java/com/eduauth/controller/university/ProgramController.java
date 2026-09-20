package com.eduauth.controller.university;

import com.eduauth.dto.program.*;
import com.eduauth.model.Institution;
import com.eduauth.model.User;
import com.eduauth.repository.InstitutionRepository;
import com.eduauth.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing the academic program structure (certificate levels,
 * departments, and programs) under the authenticated university account.
 *
 * All routes require ROLE_UNIVERSITY (enforced via @PreAuthorize).
 *
 * Routes:
 *   GET    /api/university/program-structure           → full nested structure
 *   POST   /api/university/certificate-levels          → create certificate level
 *   PUT    /api/university/certificate-levels/{id}     → edit certificate level
 *   DELETE /api/university/certificate-levels/{id}     → soft-delete certificate level
 *   POST   /api/university/departments                 → create department
 *   PUT    /api/university/departments/{id}            → edit department
 *   POST   /api/university/programs                    → create program
 *   PUT    /api/university/programs/{id}               → edit program
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('UNIVERSITY')")
public class ProgramController {

    private final ProgramService programService;
    private final InstitutionRepository institutionRepository;

    // ── Helper ────────────────────────────────────────────────────────────────

    private Institution resolveInstitution(User user) {
        return institutionRepository.findByUserId(user.getId()).orElse(null);
    }

    private ResponseEntity<?> institutionNotFound() {
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Institution profile not found."
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/university/program-structure
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/university/program-structure")
    public ResponseEntity<?> getProgramStructure(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal User user) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        ProgramStructureResponse structure = programService.getProgramStructure(inst.getId(), includeInactive);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Program structure retrieved successfully.",
                "data", structure
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CERTIFICATE LEVEL endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/university/certificate-levels")
    public ResponseEntity<?> createCertificateLevel(
            @Valid @RequestBody CreateCertificateLevelRequest request,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        var result = programService.createCertificateLevel(request, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Certificate level created successfully.",
                "data", result
        ));
    }

    @RequestMapping(value = "/api/university/certificate-levels/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateCertificateLevel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCertificateLevelRequest request,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        var result = programService.updateCertificateLevel(id, request, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Certificate level updated successfully.",
                "data", result
        ));
    }

    @DeleteMapping("/api/university/certificate-levels/{id}")
    public ResponseEntity<?> deleteCertificateLevel(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        programService.deleteCertificateLevel(id, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Certificate level deactivated successfully."
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEPARTMENT endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/university/departments")
    public ResponseEntity<?> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        var result = programService.createDepartment(request, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Department created successfully.",
                "data", result
        ));
    }

    @RequestMapping(value = "/api/university/departments/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        var result = programService.updateDepartment(id, request, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Department updated successfully.",
                "data", result
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRAM endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/university/programs")
    public ResponseEntity<?> createProgram(
            @Valid @RequestBody CreateProgramRequest request,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        var result = programService.createProgram(request, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Program created successfully.",
                "data", result
        ));
    }

    @RequestMapping(value = "/api/university/programs/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProgramRequest request,
            @AuthenticationPrincipal User user) {

        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        var result = programService.updateProgram(id, request, inst.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Program updated successfully.",
                "data", result
        ));
    }
}
