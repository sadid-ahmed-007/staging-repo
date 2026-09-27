# EduAuth Registry — Project Context

## Tech Stack
**Backend**: Spring Boot 3.3.2, Java 17, Maven 3.9.6
**Frontend**: React 18.3.1, Vite 5.4.19, Tailwind CSS 3.4.17
**Database**: MySQL 8.0+
**Auth**: JWT (jjwt 0.12.6)
**Other Key Libraries**: iText 7.2.5 (PDF), ZXing 3.5.3 (QR), JavaMail, react-router-dom 6.30.1, axios 1.11.0

## Project Structure
```text
eduauth-registry-maven/
-- db/
   +-- schema.sql
-- backend/
   -- pom.xml
   +-- src/main/
       -- java/com/eduauth/
          -- config/
          -- controller/
          -- dto/
          -- exception/
          -- model/
          -- repository/
          -- security/
          +-- service/
       +-- resources/
           +-- application.properties
-- frontend/
   -- package.json
   +-- src/
       -- components/
       -- contexts/
       -- pages/
       -- services/
       +-- App.jsx
```

## User Roles
- **Student**: Manages their enrollments, applies to universities, manages access to their certificates, and downloads/shares issued certificates.
- **University**: Admits students, manages academic enrollments, and issues digital certificates (single and batch).
- **Verifier**: Third-party entities (employers, agencies) that manually verify certificates or request explicit access from students.
- **Admin**: Superusers who manage the system, approve new registrations, revoke suspicious certificates, and oversee system health.

## Critical Rules
```text
- NO cookies, NO sessions, NO CSRF tokens
- JWT stored in localStorage, sent as Authorization: Bearer <token>
- Passwords hashed with BCrypt
- NID stored as SHA-256 hash only, never plain text
- All accounts need admin approval before login works
- A certificate can only be issued to an enrolled student
- One active enrollment per student at a time across all universities
- Anonymous verification can be blocked per certificate by student
- allowAnonymousVerification=false returns not_found (not an error)
```

## Database Tables
- `users`: Core user accounts and authentication roles
- `password_reset_tokens`: Tokens for user password resets
- `personal_access_tokens`: API authentication tokens (Sanctum/JWT equivalents)
- `pending_registrations`: Email verification OTPs and temporary signup data
- `students`: Student profiles and hashed/encrypted NID credentials
- `institutions`: University and academic institution profiles
- `certificate_levels`: Degree levels (BSc, MSc, etc.) offered by institutions
- `departments`: Academic departments linked to institutions
- `majors`: Academic majors within departments
- `programs`: Academic degree programs
- `verifiers`: Employer and background check verifier profiles
- `enrollments`: Student university enrollments and academic standing
- `certificate_sequences`: Serial number sequence tracking per prefix/year
- `certificates`: Issued academic certificates and revocation state
- `certificate_access_requests`: Access requests submitted by verifiers to students
- `verifier_access`: Active, expired, or revoked verifier access grants
- `verification_logs`: Audit trail of certificate verification attempts
- `activity_logs`: Comprehensive system event and user action audit trail
- `jobs` / `failed_jobs`: Active and failed background queue jobs
- `withdrawal_requests`: Requests submitted by students to withdraw from enrollments
- `profile_change_requests`: Student requests for sensitive profile updates
- `notifications`: User notifications (system alerts, events)
- `user_settings`: User preferences and privacy controls
- `enrollment_applications`: Applications submitted by prospective students
- `extension_requests`: Graduation date extension requests
- `program_change_requests`: Department or major transfer requests
- `university_applications`: Stores student applications to universities
- `account_deletion_requests`: Tracks account deletion requests

## API Summary
**Auth & Public**
- POST `/api/auth/register` — Register a new account
- POST `/api/auth/verify-email` — Verify email OTP
- POST `/api/auth/login` — Login to receive JWT
- POST `/api/auth/logout` — Logout user
- GET `/api/auth/me` — Fetch current user details
- GET `/api/verify` — Verify a certificate by serial and DOB
- GET `/api/verify-link` — Verify using a secure share link
- GET `/api/public/universities` — List participating universities

**Student**
- GET `/api/student/dashboard` — Get student dashboard statistics
- GET `/api/student/certificates` — List student certificates
- POST `/api/student/certificates/{id}/privacy` — Update certificate privacy settings
- GET `/api/student/enrollments` — List enrollments
- POST `/api/student/applications` — Apply to a university
- GET `/api/student/access-requests` — List verifier access requests
- POST `/api/student/access-requests/{id}/approve` — Approve verifier access request
- POST `/api/student/profile-change` — Request a profile update

**University**
- GET `/api/university/dashboard` — Get university dashboard statistics
- GET `/api/university/students/search` — Search enrolled students
- POST `/api/university/enrollments` — Enroll a student
- POST `/api/university/certificates` — Issue a new certificate
- POST `/api/university/certificates/batch` — Batch issue certificates via CSV
- GET `/api/university/applications` — View pending applications
- POST `/api/university/applications/{id}/review` — Accept/Reject an application
- POST `/api/university/departments` — Create an academic department

**Verifier**
- GET `/api/verifier/dashboard` — Get verifier dashboard statistics
- GET `/api/verifier/search` — Search for a student by ID, email, or NID
- POST `/api/verifier/access-requests` — Send a certificate access request
- GET `/api/verifier/accessible-certificates/{studentId}` — View granted certificates
- GET `/api/verifier/verifications/history` — View past verification logs

**Admin**
- GET `/api/admin/dashboard` — Get admin dashboard statistics
- GET `/api/admin/users` — List pending/registered users
- POST `/api/admin/users/{id}/approve` — Approve a pending user account
- GET `/api/admin/certificates` — List all certificates
- POST `/api/admin/certificates/{id}/revoke` — Revoke a certificate globally
- GET `/api/admin/profile-changes` — Review profile change requests

## Frontend Routes
- `/` — Landing
- `/login` — Login
- `/register` / `/register/:role` — RegisterLanding / Register
- `/email-verification` — EmailVerification
- `/verify` — VerifyCertificate
- `/student/dashboard` — StudentDashboard
- `/student/certificates` — StudentCertificates
- `/student/applications` — StudentApplications
- `/university/dashboard` — UniversityDashboard
- `/university/enrollments` — Enrollments
- `/university/issue-certificate` — IssueCertificate
- `/verifier/dashboard` — VerifierDashboard
- `/verifier/search` — StudentSearch
- `/admin/dashboard` — AdminDashboard
- `/admin/users` — AdminUsers

## Key Services
- `AccessService`: Manages verifier access requests and grants to student certificates.
- `AdminUserService`: Handles administrative actions on user accounts (approvals, suspensions).
- `AnalyticsService`: Generates system-wide analytics data for dashboards.
- `ApplicationService`: Manages the lifecycle of student enrollment applications.
- `AuthService`: Core authentication logic, OTP generation, and registration.
- `CertificateService`: Issues, revokes, and formats certificates, including PDF operations.
- `CustomUserDetailsService`: Connects Spring Security to the database `users` table.
- `DashboardService`: Aggregates metrics for the different role-based dashboards.
- `EmailService`: Dispatches emails for OTPs, approvals, and system notifications.
- `EncryptionService`: Manages AES encryption and SHA hashing for NIDs and sensitive data.
- `EnrollmentService`: Oversees student enrollments, statuses, and graduations.
- `FileStorageService`: Handles upload/download of PDFs, avatars, and CSVs.
- `JwtService`: Generates, parses, and validates JSON Web Tokens.
- `NotificationService`: Internal logic for creating and retrieving user notifications.
- `ProgramService`: Manages the academic hierarchy (levels, departments, programs).
- `ScheduledTaskService`: Handles automated background jobs and cleanup tasks.
- `SerialGeneratorService`: Assigns unique, sequential serial numbers to new certificates.
- `TokenBlacklistService`: Manages JWT invalidation upon logout.
- `VerificationNotificationService`: Triggers alerts upon successful or failed certificate verifications.

## Email Events
- Registration OTP verification
- Account approval by Admin
- Certificate issued by University
- Verifier requests access
- Access granted by Student
- Application accepted by University
- Certificate revoked

## Notification Types
- ACCOUNT_APPROVED
- NEW_CERTIFICATE_ISSUED
- ACCESS_REQUEST_RECEIVED
- ACCESS_REQUEST_APPROVED
- APPLICATION_ACCEPTED
- APPLICATION_REJECTED
- CERTIFICATE_REVOKED
- SYSTEM_ALERT

## Test Accounts
- **Admin**: `admin@eduauth.com` / `admin123`
- **Student**: `student@test.com` / `password123`
- **University**: `demo@uiu.ac.bd` / `password123`
- **Verifier**: `verifier@company.com` / `password123`

## Current Status
- Project base schema, Maven pom, and Spring Boot structural configuration are fully in place.
- All core controllers have been scaffolded and structured properly for Postman generation.
- We are currently reviewing backend functionality to ensure DTO and entity mapping behaves perfectly without Laravel-isms.
- **Done**: DB Schema, Tech Stack definition, initial Controllers and Services outline.
- **Not Yet Done**: Full end-to-end integration with Frontend elements, rigorous PDF templating for iText, robust exception handling across all edges.

## Common Mistakes to Avoid
- Do not add CSRF tokens
- Do not set withCredentials: true in Axios
- Do not copy Laravel-specific patterns to Spring Boot
- When fixing frontend, check if api.js interceptor returns response.data or response — be consistent
- Anonymous verification checks happen BEFORE DOB check (privacy)
- revokedByRole must be checked before allowing university to revalidate
- @Builder.Default required for fields with default values in DTO classes
- JPA entity classes should NOT use @Builder
- allowAnonymousVerification=false must return not_found not 403
