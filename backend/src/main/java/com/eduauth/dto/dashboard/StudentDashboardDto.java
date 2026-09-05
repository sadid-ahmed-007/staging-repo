package com.eduauth.dto.dashboard;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentDashboardDto {
    private long totalCertificates;
    private long publicCertificates;
    private long privateCertificates;
    private long pendingAccessRequests;
    private long activeAccessGrants;
    private CurrentEnrollmentDto currentEnrollment;

    @Data
    public static class CurrentEnrollmentDto {
        private String institutionName;
        private String enrollmentNumber;
        private String program;
        private String batch;
        private String status;
        private LocalDate enrollmentDate;
        private LocalDate expectedGraduationDate;
    }
}
