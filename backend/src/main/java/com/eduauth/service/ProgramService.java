package com.eduauth.service;

import com.eduauth.dto.program.*;
import com.eduauth.exception.BadRequestException;
import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.CertificateLevel;
import com.eduauth.model.Department;
import com.eduauth.model.Program;
import com.eduauth.repository.CertificateLevelRepository;
import com.eduauth.repository.DepartmentRepository;
import com.eduauth.repository.EnrollmentRepository;
import com.eduauth.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramService {

        private final CertificateLevelRepository certificateLevelRepository;
        private final DepartmentRepository departmentRepository;
        private final ProgramRepository programRepository;
        private final EnrollmentRepository enrollmentRepository;

        // ─────────────────────────────────────────────────────────────────────────
        // PROGRAM STRUCTURE (nested)
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Returns the full nested program structure for a university.
         * Only includes active certificate levels, departments, and programs by
         * default.
         */
        @Transactional(readOnly = true)
        public ProgramStructureResponse getProgramStructure(Long institutionId) {
                return getProgramStructure(institutionId, false);
        }

        @Transactional(readOnly = true)
        public ProgramStructureResponse getProgramStructure(Long institutionId, boolean includeInactive) {
                // Load certificate levels
                List<CertificateLevel> levels = includeInactive
                                ? certificateLevelRepository.findByInstitutionIdOrderByNameAsc(institutionId)
                                : certificateLevelRepository
                                                .findByInstitutionIdAndIsActiveTrueOrderByNameAsc(institutionId);

                // Load departments for this institution
                List<Department> allDepts = includeInactive
                                ? departmentRepository.findByInstitutionId(institutionId)
                                : departmentRepository.findByInstitutionIdAndIsActiveTrue(institutionId);

                // Load programs for this institution
                List<Program> allPrograms = includeInactive
                                ? programRepository.findByUniversityId(institutionId)
                                : programRepository.findByUniversityIdAndIsActiveTrue(institutionId);

                // Index departments by certificateLevelId
                Map<Long, List<Department>> deptsByLevel = allDepts.stream()
                                .filter(d -> d.getCertificateLevelId() != null)
                                .collect(Collectors.groupingBy(Department::getCertificateLevelId));

                // Index programs by departmentId
                Map<Long, List<Program>> programsByDept = allPrograms.stream()
                                .collect(Collectors.groupingBy(Program::getDepartmentId));

                // Build nested response
                List<ProgramStructureResponse.CertificateLevelDto> levelDtos = levels.stream()
                                .map(level -> {
                                        List<Department> depts = deptsByLevel.getOrDefault(level.getId(), List.of());

                                        List<ProgramStructureResponse.DepartmentDto> deptDtos = depts.stream()
                                                        .map(dept -> {
                                                                List<Program> programs = programsByDept
                                                                                .getOrDefault(dept.getId(), List.of());

                                                                List<ProgramStructureResponse.ProgramDto> programDtos = programs
                                                                                .stream()
                                                                                .map(p -> ProgramStructureResponse.ProgramDto
                                                                                                .builder()
                                                                                                .id(p.getId())
                                                                                                .name(p.getName())
                                                                                                .shortName(p.getShortName())
                                                                                                .departmentId(p.getDepartmentId())
                                                                                                .universityId(p.getUniversityId())
                                                                                                .isActive(p.getIsActive())
                                                                                                .build())
                                                                                .collect(Collectors.toList());

                                                                return ProgramStructureResponse.DepartmentDto.builder()
                                                                                .id(dept.getId())
                                                                                .name(dept.getName())
                                                                                .code(dept.getCode() != null
                                                                                                ? dept.getCode()
                                                                                                : dept.getShortCode())
                                                                                .certificateLevelId(dept
                                                                                                .getCertificateLevelId())
                                                                                .isActive(dept.getIsActive())
                                                                                .programs(programDtos)
                                                                                .build();
                                                        })
                                                        .collect(Collectors.toList());

                                        return ProgramStructureResponse.CertificateLevelDto.builder()
                                                        .id(level.getId())
                                                        .name(level.getName())
                                                        .shortName(level.getShortCode())
                                                        .serialPrefix(level.getSerialPrefix())
                                                        .durationYears(level.getDurationYears())
                                                        .isActive(level.getIsActive())
                                                        .departments(deptDtos)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return ProgramStructureResponse.builder()
                                .certificateLevels(levelDtos)
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // CERTIFICATE LEVEL CRUD
        // ─────────────────────────────────────────────────────────────────────────

        @Transactional
        public ProgramStructureResponse.CertificateLevelDto createCertificateLevel(
                        CreateCertificateLevelRequest request, Long institutionId) {

                CertificateLevel level = new CertificateLevel();
                level.setInstitutionId(institutionId);
                level.setName(request.getName().trim());
                level.setShortCode(request.getShortName().trim());
                String prefix = (request.getSerialPrefix() != null && !request.getSerialPrefix().isBlank())
                                ? request.getSerialPrefix().trim().toUpperCase()
                                : (request.getShortName() != null ? request.getShortName().trim().toUpperCase()
                                                : "CERT");
                level.setSerialPrefix(prefix);
                level.setDurationYears(request.getDurationYears() != null ? request.getDurationYears() : 4);
                level.setIsActive(true);
                level = certificateLevelRepository.save(level);

                log.info("Created certificate level '{}' for institution {}", level.getName(), institutionId);

                return ProgramStructureResponse.CertificateLevelDto.builder()
                                .id(level.getId())
                                .name(level.getName())
                                .shortName(level.getShortCode())
                                .serialPrefix(level.getSerialPrefix())
                                .durationYears(level.getDurationYears())
                                .isActive(level.getIsActive())
                                .departments(List.of())
                                .build();
        }

        @Transactional
        public ProgramStructureResponse.CertificateLevelDto updateCertificateLevel(
                        Long id, UpdateCertificateLevelRequest request, Long institutionId) {

                CertificateLevel level = certificateLevelRepository.findByIdAndInstitutionId(id, institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Certificate level not found."));

                if (request.getName() != null && !request.getName().isBlank()) {
                        level.setName(request.getName().trim());
                }
                if (request.getShortName() != null && !request.getShortName().isBlank()) {
                        level.setShortCode(request.getShortName().trim());
                }
                if (request.getSerialPrefix() != null && !request.getSerialPrefix().isBlank()) {
                        level.setSerialPrefix(request.getSerialPrefix().trim().toUpperCase());
                }
                if (request.getDurationYears() != null) {
                        level.setDurationYears(request.getDurationYears());
                }
                if (request.getIsActive() != null) {
                        if (!request.getIsActive()) {
                                boolean hasActiveEnrollments = enrollmentRepository
                                                .existsByCertificateLevelIdAndStatus(id, "active");
                                if (hasActiveEnrollments) {
                                        throw new BadRequestException(
                                                        "Cannot deactivate this certificate level: there are active enrollments using it.");
                                }
                        }
                        level.setIsActive(request.getIsActive());
                }
                level = certificateLevelRepository.save(level);

                log.info("Updated certificate level {} for institution {}", id, institutionId);

                return ProgramStructureResponse.CertificateLevelDto.builder()
                                .id(level.getId())
                                .name(level.getName())
                                .shortName(level.getShortCode())
                                .serialPrefix(level.getSerialPrefix())
                                .durationYears(level.getDurationYears())
                                .isActive(level.getIsActive())
                                .departments(List.of())
                                .build();
        }

        /**
         * Soft-deletes a certificate level by setting isActive=false.
         * Blocked if there are active enrollments using this level.
         */
        @Transactional
        public void deleteCertificateLevel(Long id, Long institutionId) {
                CertificateLevel level = certificateLevelRepository.findByIdAndInstitutionId(id, institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Certificate level not found."));

                // Guard: check if there are active enrollments using this level
                boolean hasActiveEnrollments = enrollmentRepository
                                .existsByCertificateLevelIdAndStatus(id, "active");
                if (hasActiveEnrollments) {
                        throw new BadRequestException(
                                        "Cannot deactivate this certificate level: there are active enrollments using it.");
                }

                level.setIsActive(false);
                certificateLevelRepository.save(level);
                log.info("Soft-deleted certificate level {} for institution {}", id, institutionId);
        }

        // ─────────────────────────────────────────────────────────────────────────
        // DEPARTMENT CRUD
        // ─────────────────────────────────────────────────────────────────────────

        @Transactional
        public ProgramStructureResponse.DepartmentDto createDepartment(
                        CreateDepartmentRequest request, Long institutionId) {

                // Verify the certificate level belongs to this institution
                certificateLevelRepository.findByIdAndInstitutionId(request.getCertificateLevelId(), institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Certificate level not found for this institution."));

                Department dept = new Department();
                dept.setInstitutionId(institutionId);
                dept.setCertificateLevelId(request.getCertificateLevelId());
                dept.setName(request.getName().trim());
                dept.setCode(request.getCode().trim().toUpperCase());
                dept.setShortCode(request.getCode().trim().toUpperCase()); // keep shortCode in sync
                dept.setIsActive(true);
                dept = departmentRepository.save(dept);

                log.info("Created department '{}' under cert level {} for institution {}",
                                dept.getName(), request.getCertificateLevelId(), institutionId);

                return ProgramStructureResponse.DepartmentDto.builder()
                                .id(dept.getId())
                                .name(dept.getName())
                                .code(dept.getCode())
                                .certificateLevelId(dept.getCertificateLevelId())
                                .isActive(dept.getIsActive())
                                .programs(List.of())
                                .build();
        }

        @Transactional
        public ProgramStructureResponse.DepartmentDto updateDepartment(
                        Long id, UpdateDepartmentRequest request, Long institutionId) {

                Department dept = departmentRepository.findByIdAndInstitutionId(id, institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

                if (request.getName() != null && !request.getName().isBlank()) {
                        dept.setName(request.getName().trim());
                }
                if (request.getCode() != null && !request.getCode().isBlank()) {
                        String code = request.getCode().trim().toUpperCase();
                        dept.setCode(code);
                        dept.setShortCode(code); // keep in sync
                }
                if (request.getIsActive() != null) {
                        dept.setIsActive(request.getIsActive());
                }
                dept = departmentRepository.save(dept);

                log.info("Updated department {} for institution {}", id, institutionId);

                return ProgramStructureResponse.DepartmentDto.builder()
                                .id(dept.getId())
                                .name(dept.getName())
                                .code(dept.getCode())
                                .certificateLevelId(dept.getCertificateLevelId())
                                .isActive(dept.getIsActive())
                                .programs(List.of())
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // PROGRAM CRUD
        // ─────────────────────────────────────────────────────────────────────────

        @Transactional
        public ProgramStructureResponse.ProgramDto createProgram(
                        CreateProgramRequest request, Long institutionId) {

                // Verify department belongs to this institution
                Department dept = departmentRepository
                                .findByIdAndInstitutionId(request.getDepartmentId(), institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Department not found for this institution."));

                Program program = new Program();
                program.setDepartmentId(dept.getId());
                program.setUniversityId(institutionId);
                program.setName(request.getName().trim());
                program.setShortName(request.getShortName() != null ? request.getShortName().trim() : null);
                program.setIsActive(true);
                program = programRepository.save(program);

                log.info("Created program '{}' under department {} for institution {}",
                                program.getName(), dept.getId(), institutionId);

                return ProgramStructureResponse.ProgramDto.builder()
                                .id(program.getId())
                                .name(program.getName())
                                .shortName(program.getShortName())
                                .departmentId(program.getDepartmentId())
                                .universityId(program.getUniversityId())
                                .isActive(program.getIsActive())
                                .build();
        }

        @Transactional
        public ProgramStructureResponse.ProgramDto updateProgram(
                        Long id, UpdateProgramRequest request, Long institutionId) {

                Program program = programRepository.findByIdAndUniversityId(id, institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Program not found."));

                if (request.getName() != null && !request.getName().isBlank()) {
                        program.setName(request.getName().trim());
                }
                if (request.getShortName() != null) {
                        program.setShortName(request.getShortName().trim());
                }
                if (request.getIsActive() != null) {
                        program.setIsActive(request.getIsActive());
                }
                program = programRepository.save(program);

                log.info("Updated program {} for institution {}", id, institutionId);

                return ProgramStructureResponse.ProgramDto.builder()
                                .id(program.getId())
                                .name(program.getName())
                                .shortName(program.getShortName())
                                .departmentId(program.getDepartmentId())
                                .universityId(program.getUniversityId())
                                .isActive(program.getIsActive())
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // HELPERS (for use by EnrollmentService and certificate issuance)
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Load a program by ID and verify it belongs to the institution.
         */
        public Program findProgramForInstitution(Long programId, Long institutionId) {
                return programRepository.findByIdAndUniversityId(programId, institutionId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Program not found or does not belong to your institution."));
        }

        /**
         * Given a programId, navigate up to the certificate level and return duration
         * in years.
         * Returns null if navigation fails.
         */
        public Integer getProgramDurationYears(Long programId) {
                return programRepository.findById(programId)
                                .flatMap(p -> departmentRepository.findById(p.getDepartmentId()))
                                .flatMap(d -> d.getCertificateLevelId() != null
                                                ? certificateLevelRepository.findById(d.getCertificateLevelId())
                                                : Optional.empty())
                                .map(CertificateLevel::getDurationYears)
                                .orElse(null);
        }

        /**
         * Resolve department and certificate level for a given programId.
         * Used by certificate issuance to auto-populate fields.
         */
        public ProgramHierarchy resolveProgramHierarchy(Long programId) {
                Program program = programRepository.findById(programId)
                                .orElseThrow(() -> new ResourceNotFoundException("Program not found."));

                Department dept = departmentRepository.findById(program.getDepartmentId())
                                .orElseThrow(() -> new ResourceNotFoundException("Department not found for program."));

                CertificateLevel certLevel = dept.getCertificateLevelId() != null
                                ? certificateLevelRepository.findById(dept.getCertificateLevelId())
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Certificate level not found for department."))
                                : null;

                return new ProgramHierarchy(program, dept, certLevel);
        }

        /** Value object carrying the resolved program hierarchy. */
        public record ProgramHierarchy(Program program, Department department, CertificateLevel certificateLevel) {
        }
}
