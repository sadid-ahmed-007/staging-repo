package com.eduauth.dto.program;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Nested response DTO for the program structure endpoint.
 *
 * Shape:
 * {
 *   "certificateLevels": [
 *     {
 *       "id": 1,
 *       "name": "Bachelor of Science",
 *       "shortName": "BSc",
 *       "serialPrefix": "BSC",
 *       "durationYears": 4,
 *       "departments": [
 *         {
 *           "id": 1,
 *           "name": "Computer Science and Engineering",
 *           "code": "CSE",
 *           "programs": [
 *             { "id": 1, "name": "BSc in CSE", "shortName": "BSc in CSE" }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 */
@Data
@Builder
public class ProgramStructureResponse {

    private List<CertificateLevelDto> certificateLevels;

    @Data
    @Builder
    public static class CertificateLevelDto {
        private Long id;
        private String name;
        private String shortName;
        private String serialPrefix;
        private Integer durationYears;
        private Boolean isActive;
        private List<DepartmentDto> departments;
    }

    @Data
    @Builder
    public static class DepartmentDto {
        private Long id;
        private String name;
        private String code;
        private Long certificateLevelId;
        private Boolean isActive;
        private List<ProgramDto> programs;
    }

    @Data
    @Builder
    public static class ProgramDto {
        private Long id;
        private String name;
        private String shortName;
        private Long departmentId;
        private Long universityId;
        private Boolean isActive;
    }
}
