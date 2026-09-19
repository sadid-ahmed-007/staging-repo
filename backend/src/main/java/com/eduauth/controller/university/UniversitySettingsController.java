package com.eduauth.controller.university;

import com.eduauth.model.*;
import com.eduauth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
@PreAuthorize("hasRole('UNIVERSITY')")
public class UniversitySettingsController {

    private final InstitutionRepository institutionRepository;
    private final CertificateLevelRepository certificateLevelRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;

    private Institution resolveInstitution(User user) {
        return institutionRepository.findByUserId(user.getId()).orElse(null);
    }

    // --- Certificate Levels ---

    @GetMapping("/certificate-levels")
    public ResponseEntity<?> getCertificateLevels(@AuthenticationPrincipal User user) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        List<CertificateLevel> levels = certificateLevelRepository.findByInstitutionId(inst.getId());
        return ResponseEntity.ok(Map.of("success", true, "certificate_levels", levels));
    }

    @PostMapping("/certificate-levels")
    @Transactional
    public ResponseEntity<?> createCertificateLevel(@AuthenticationPrincipal User user, @RequestBody Map<String, String> body) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        CertificateLevel level = new CertificateLevel();
        level.setInstitutionId(inst.getId());
        level.setName(body.get("name"));
        level.setShortCode(body.get("short_code"));
        level.setIsActive(true);
        certificateLevelRepository.save(level);

        return ResponseEntity.ok(Map.of("success", true, "message", "Certificate level created"));
    }

    @PutMapping("/certificate-levels/{id}")
    @Transactional
    public ResponseEntity<?> updateCertificateLevel(@AuthenticationPrincipal User user, @PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<CertificateLevel> levelOpt = certificateLevelRepository.findByIdAndInstitutionId(id, inst.getId());
        if (levelOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));

        CertificateLevel level = levelOpt.get();
        level.setName(body.get("name"));
        level.setShortCode(body.get("short_code"));
        certificateLevelRepository.save(level);

        return ResponseEntity.ok(Map.of("success", true, "message", "Certificate level updated"));
    }

    @DeleteMapping("/certificate-levels/{id}")
    @Transactional
    public ResponseEntity<?> deactivateCertificateLevel(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<CertificateLevel> levelOpt = certificateLevelRepository.findByIdAndInstitutionId(id, inst.getId());
        if (levelOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));

        CertificateLevel level = levelOpt.get();
        level.setIsActive(false);
        certificateLevelRepository.save(level);

        return ResponseEntity.ok(Map.of("success", true, "message", "Certificate level deactivated"));
    }

    @PostMapping("/certificate-levels/{id}/reactivate")
    @Transactional
    public ResponseEntity<?> reactivateCertificateLevel(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<CertificateLevel> levelOpt = certificateLevelRepository.findByIdAndInstitutionId(id, inst.getId());
        if (levelOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));

        CertificateLevel level = levelOpt.get();
        level.setIsActive(true);
        certificateLevelRepository.save(level);

        return ResponseEntity.ok(Map.of("success", true, "message", "Certificate level reactivated"));
    }

    // --- Departments ---

    @GetMapping("/departments")
    public ResponseEntity<?> getDepartments(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, name = "certificate_level_id") Long levelId) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        List<Department> depts;
        if (levelId != null) {
            depts = departmentRepository.findByInstitutionIdAndCertificateLevelId(inst.getId(), levelId);
        } else {
            depts = departmentRepository.findByInstitutionId(inst.getId());
        }
        return ResponseEntity.ok(Map.of("success", true, "departments", depts));
    }

    @PostMapping("/departments")
    @Transactional
    public ResponseEntity<?> createDepartment(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Department dept = new Department();
        dept.setInstitutionId(inst.getId());
        dept.setName((String) body.get("name"));
        dept.setShortCode((String) body.get("short_code"));
        if (body.get("certificate_level_id") != null) {
            dept.setCertificateLevelId(Long.valueOf(body.get("certificate_level_id").toString()));
        }
        dept.setIsActive(true);
        departmentRepository.save(dept);

        return ResponseEntity.ok(Map.of("success", true, "message", "Department created"));
    }

    @PutMapping("/departments/{id}")
    @Transactional
    public ResponseEntity<?> updateDepartment(@AuthenticationPrincipal User user, @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(id, inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));

        Department dept = deptOpt.get();
        dept.setName((String) body.get("name"));
        dept.setShortCode((String) body.get("short_code"));
        if (body.get("certificate_level_id") != null) {
            dept.setCertificateLevelId(Long.valueOf(body.get("certificate_level_id").toString()));
        }
        departmentRepository.save(dept);

        return ResponseEntity.ok(Map.of("success", true, "message", "Department updated"));
    }

    @DeleteMapping("/departments/{id}")
    @Transactional
    public ResponseEntity<?> deactivateDepartment(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(id, inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));

        Department dept = deptOpt.get();
        dept.setIsActive(false);
        departmentRepository.save(dept);

        return ResponseEntity.ok(Map.of("success", true, "message", "Department deactivated"));
    }

    @PostMapping("/departments/{id}/reactivate")
    @Transactional
    public ResponseEntity<?> reactivateDepartment(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(id, inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));

        Department dept = deptOpt.get();
        dept.setIsActive(true);
        departmentRepository.save(dept);

        return ResponseEntity.ok(Map.of("success", true, "message", "Department reactivated"));
    }

    // --- Majors ---

    @GetMapping("/majors")
    public ResponseEntity<?> getMajors(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, name = "department_id") Long deptId) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        // Security check: ensure department belongs to institution
        if (deptId != null) {
            Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(deptId, inst.getId());
            if (deptOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "majors", List.of())); // empty
            }
            List<Major> majors = majorRepository.findByDepartmentId(deptId);
            return ResponseEntity.ok(Map.of("success", true, "majors", majors));
        }

        return ResponseEntity.ok(Map.of("success", true, "majors", List.of())); // need department to fetch majors realistically
    }

    @PostMapping("/majors")
    @Transactional
    public ResponseEntity<?> createMajor(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Long deptId = Long.valueOf(body.get("department_id").toString());
        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(deptId, inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Department not found"));

        Major major = new Major();
        major.setDepartmentId(deptId);
        major.setName((String) body.get("name"));
        major.setIsActive(true);
        majorRepository.save(major);

        return ResponseEntity.ok(Map.of("success", true, "message", "Major created"));
    }

    @PutMapping("/majors/{id}")
    @Transactional
    public ResponseEntity<?> updateMajor(@AuthenticationPrincipal User user, @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<Major> majorOpt = majorRepository.findById(id);
        if (majorOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));
        
        Major major = majorOpt.get();
        // check ownership
        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(major.getDepartmentId(), inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));

        major.setName((String) body.get("name"));
        majorRepository.save(major);

        return ResponseEntity.ok(Map.of("success", true, "message", "Major updated"));
    }

    @DeleteMapping("/majors/{id}")
    @Transactional
    public ResponseEntity<?> deactivateMajor(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<Major> majorOpt = majorRepository.findById(id);
        if (majorOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));
        
        Major major = majorOpt.get();
        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(major.getDepartmentId(), inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));

        major.setIsActive(false);
        majorRepository.save(major);

        return ResponseEntity.ok(Map.of("success", true, "message", "Major deactivated"));
    }

    @PostMapping("/majors/{id}/reactivate")
    @Transactional
    public ResponseEntity<?> reactivateMajor(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        Institution inst = resolveInstitution(user);
        if (inst == null) return institutionNotFound();

        Optional<Major> majorOpt = majorRepository.findById(id);
        if (majorOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("success", false, "message", "Not found"));
        
        Major major = majorOpt.get();
        Optional<Department> deptOpt = departmentRepository.findByIdAndInstitutionId(major.getDepartmentId(), inst.getId());
        if (deptOpt.isEmpty()) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));

        major.setIsActive(true);
        majorRepository.save(major);

        return ResponseEntity.ok(Map.of("success", true, "message", "Major reactivated"));
    }

    private ResponseEntity<?> institutionNotFound() {
        return ResponseEntity.status(404).body(Map.of("success", false, "message", "Institution profile not found"));
    }
}
