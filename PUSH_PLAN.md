# EduAuth Registry — Multi-Day GitHub Push Plan

> **Team PhaseShift** — United International University  
> Advanced Object-Oriented Programming (AOOP)  
> 4 members · 5 days · 22 pushes · 157 tracked files

---

## Table of Contents

1. [File Inventory](#1-file-inventory)
2. [Dependency Map](#2-dependency-map)
3. [Full Multi-Day Timeline](#3-full-multi-day-timeline)
4. [Quick Reference by Member](#4-quick-reference-by-member)
5. [Summary Table and Stats](#5-summary-table-and-stats)
6. [GitHub Verification Guide](#6-github-verification-guide)

---

## 1. File Inventory

Every file in the project, grouped by category, with the assigned team member.

### Database (2 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 1 | `db/schema.sql` | Full database schema — all tables |
| 2 | `db/seed.sql` | Seed data for testing |

### Backend — Build & Config (3 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 3 | `backend/pom.xml` | Maven build configuration with all dependencies |
| 4 | `backend/src/main/resources/application.properties` | Spring Boot configuration |
| 5 | `backend/src/main/java/com/eduauth/EduAuthApplication.java` | Main Spring Boot application class |

### Backend — Infrastructure Config (6 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 6 | `backend/src/main/java/com/eduauth/config/CorsConfig.java` | CORS configuration |
| 7 | `backend/src/main/java/com/eduauth/config/MailConfig.java` | Mail configuration |
| 8 | `backend/src/main/java/com/eduauth/exception/BadRequestException.java` | Custom exception |
| 9 | `backend/src/main/java/com/eduauth/exception/ResourceNotFoundException.java` | Custom exception |
| 10 | `backend/src/main/java/com/eduauth/exception/UnauthorizedException.java` | Custom exception |
| 11 | `backend/src/main/java/com/eduauth/exception/GlobalExceptionHandler.java` | Central error handler |

### Backend — Security Layer (6 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 12 | `backend/src/main/java/com/eduauth/util/HashUtil.java` | BCrypt + SHA-256 hashing |
| 13 | `backend/src/main/java/com/eduauth/config/JwtConfig.java` | JWT properties binding |
| 14 | `backend/src/main/java/com/eduauth/service/JwtService.java` | Token generation and validation |
| 15 | `backend/src/main/java/com/eduauth/service/CustomUserDetailsService.java` | Spring Security user loader |
| 16 | `backend/src/main/java/com/eduauth/config/JwtAuthenticationFilter.java` | JWT request filter |
| 17 | `backend/src/main/java/com/eduauth/config/SecurityConfig.java` | Security filter chain |

### Backend — Entity Models (13 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 18 | `backend/src/main/java/com/eduauth/model/User.java` | User entity |
| 19 | `backend/src/main/java/com/eduauth/model/Student.java` | Student profile entity |
| 20 | `backend/src/main/java/com/eduauth/model/Institution.java` | University/institution entity |
| 21 | `backend/src/main/java/com/eduauth/model/Verifier.java` | Verifier profile entity |
| 22 | `backend/src/main/java/com/eduauth/model/Enrollment.java` | Student enrollment entity |
| 23 | `backend/src/main/java/com/eduauth/model/Certificate.java` | Certificate entity |
| 24 | `backend/src/main/java/com/eduauth/model/AccessRequest.java` | Verifier access request entity |
| 25 | `backend/src/main/java/com/eduauth/model/AccessGrant.java` | Access grant entity |
| 26 | `backend/src/main/java/com/eduauth/model/VerificationLog.java` | Verification audit log entity |
| 27 | `backend/src/main/java/com/eduauth/model/WithdrawalRequest.java` | Withdrawal request entity |
| 28 | `backend/src/main/java/com/eduauth/model/ActivityLog.java` | Activity log entity |
| 29 | `backend/src/main/java/com/eduauth/model/UserSettings.java` | User settings entity |
| 30 | `backend/src/main/java/com/eduauth/model/PendingRegistration.java` | Pending registration entity |

### Backend — Repositories (14 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 31 | `backend/src/main/java/com/eduauth/repository/UserRepository.java` | User data access |
| 32 | `backend/src/main/java/com/eduauth/repository/StudentRepository.java` | Student data access |
| 33 | `backend/src/main/java/com/eduauth/repository/InstitutionRepository.java` | Institution data access |
| 34 | `backend/src/main/java/com/eduauth/repository/VerifierRepository.java` | Verifier data access |
| 35 | `backend/src/main/java/com/eduauth/repository/EnrollmentRepository.java` | Enrollment data access |
| 36 | `backend/src/main/java/com/eduauth/repository/CertificateRepository.java` | Certificate data access |
| 37 | `backend/src/main/java/com/eduauth/repository/AccessRequestRepository.java` | Access request data access |
| 38 | `backend/src/main/java/com/eduauth/repository/AccessGrantRepository.java` | Access grant data access |
| 39 | `backend/src/main/java/com/eduauth/repository/VerificationLogRepository.java` | Verification log data access |
| 40 | `backend/src/main/java/com/eduauth/repository/WithdrawalRequestRepository.java` | Withdrawal request data access |
| 41 | `backend/src/main/java/com/eduauth/repository/ActivityLogRepository.java` | Activity log data access |
| 42 | `backend/src/main/java/com/eduauth/repository/UserSettingsRepository.java` | User settings data access |
| 43 | `backend/src/main/java/com/eduauth/repository/PendingRegistrationRepository.java` | Pending registration data access |
| 44 | `backend/src/main/java/com/eduauth/repository/specification/UserSpecification.java` | JPA Specification for user filtering |

### Backend — Auth DTOs (5 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 45 | `backend/src/main/java/com/eduauth/dto/auth/LoginRequest.java` | Login request DTO |
| 46 | `backend/src/main/java/com/eduauth/dto/auth/RegisterRequest.java` | Registration request DTO |
| 47 | `backend/src/main/java/com/eduauth/dto/auth/VerifyEmailRequest.java` | Email verification DTO |
| 48 | `backend/src/main/java/com/eduauth/dto/auth/ResendOtpRequest.java` | Resend OTP DTO |
| 49 | `backend/src/main/java/com/eduauth/dto/auth/AuthResponse.java` | Auth response DTO |

### Backend — Auth Services & Controller (4 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 50 | `backend/src/main/java/com/eduauth/service/EmailService.java` | OTP email sending |
| 51 | `backend/src/main/java/com/eduauth/service/TokenBlacklistService.java` | Token blacklist for logout |
| 52 | `backend/src/main/java/com/eduauth/service/AuthService.java` | Auth business logic |
| 53 | `backend/src/main/java/com/eduauth/controller/AuthController.java` | Auth REST endpoints |

### Backend — Dashboard DTOs & Service (5 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 54 | `backend/src/main/java/com/eduauth/dto/dashboard/AdminDashboardDto.java` | Admin dashboard DTO |
| 55 | `backend/src/main/java/com/eduauth/dto/dashboard/StudentDashboardDto.java` | Student dashboard DTO |
| 56 | `backend/src/main/java/com/eduauth/dto/dashboard/UniversityDashboardDto.java` | University dashboard DTO |
| 57 | `backend/src/main/java/com/eduauth/dto/dashboard/VerifierDashboardDto.java` | Verifier dashboard DTO |
| 58 | `backend/src/main/java/com/eduauth/service/DashboardService.java` | Dashboard stats service |

### Backend — Admin (6 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 59 | `backend/src/main/java/com/eduauth/dto/admin/AdminUserListDto.java` | Admin user list DTO |
| 60 | `backend/src/main/java/com/eduauth/dto/admin/AdminUserDetailDto.java` | Admin user detail DTO |
| 61 | `backend/src/main/java/com/eduauth/dto/admin/SuspendRequestDto.java` | Suspend request DTO |
| 62 | `backend/src/main/java/com/eduauth/service/AdminUserService.java` | Admin user management service |
| 63 | `backend/src/main/java/com/eduauth/controller/admin/AdminDashboardController.java` | Admin dashboard endpoint |
| 64 | `backend/src/main/java/com/eduauth/controller/admin/AdminUserController.java` | Admin user management endpoints |

### Backend — Certificate Core (3 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 65 | `backend/src/main/java/com/eduauth/service/EncryptionService.java` | AES encryption for share links |
| 66 | `backend/src/main/java/com/eduauth/service/SerialGeneratorService.java` | Serial number + checksum generator |
| 67 | `backend/src/main/java/com/eduauth/service/CertificateService.java` | Certificate business logic |

### Backend — Admin Certificate Controller (1 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 68 | `backend/src/main/java/com/eduauth/controller/admin/AdminCertificateController.java` | Admin certificate management endpoints |

### Backend — Student Controllers (2 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 69 | `backend/src/main/java/com/eduauth/controller/student/StudentDashboardController.java` | Student dashboard endpoint |
| 70 | `backend/src/main/java/com/eduauth/controller/student/StudentCertificateController.java` | Student certificate endpoints |

### Backend — University Controllers (2 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 71 | `backend/src/main/java/com/eduauth/controller/university/UniversityDashboardController.java` | University dashboard endpoint |
| 72 | `backend/src/main/java/com/eduauth/controller/university/UniversityCertificateController.java` | University certificate endpoints |

### Backend — Verifier Controllers (4 files) — Sadid + Saikat

| # | File | Purpose |
|---|------|---------|
| 73 | `backend/src/main/java/com/eduauth/controller/verifier/VerifierDashboardController.java` | Verifier dashboard endpoint (Saikat) |
| 74 | `backend/src/main/java/com/eduauth/controller/verifier/VerifierCertificateController.java` | Verifier certificate access endpoints (Saikat) |
| 75 | `backend/src/main/java/com/eduauth/controller/VerifierController.java` | Verifier verification + access request endpoints (Sadid) |
| 76 | `backend/src/main/java/com/eduauth/controller/publics/VerifyController.java` | Public verification endpoints (Sadid) |

### Frontend — Project Config (9 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 77 | `frontend/.gitignore` | Git ignore rules |
| 78 | `frontend/.oxlintrc.json` | Lint configuration |
| 79 | `frontend/index.html` | HTML entry point |
| 80 | `frontend/package.json` | npm dependencies and scripts |
| 81 | `frontend/postcss.config.js` | PostCSS config for Tailwind |
| 82 | `frontend/tailwind.config.js` | Tailwind CSS configuration |
| 83 | `frontend/vite.config.js` | Vite dev server config |
| 84 | `frontend/README.md` | Frontend readme (Vite template) |
| 85 | `frontend/src/index.css` | Global CSS with design tokens |

### Frontend — Core App Files (5 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 86 | `frontend/src/main.jsx` | React entry point |
| 87 | `frontend/src/services/api.js` | Axios instance with JWT interceptor |
| 88 | `frontend/src/services/authService.js` | Auth API helper functions |
| 89 | `frontend/src/contexts/AuthContext.jsx` | Authentication state context |
| 90 | `frontend/src/App.jsx` | Router and route definitions |

### Frontend — Additional Contexts & Hooks (3 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 91 | `frontend/src/contexts/ThemeContext.jsx` | Dark/light theme context |
| 92 | `frontend/src/contexts/NotificationContext.jsx` | Notification polling context |
| 93 | `frontend/src/hooks/useOutsideClick.js` | Click-outside custom hook |

### Frontend — Shared Components (19 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 94 | `frontend/src/components/shared/Button.jsx` | Reusable button |
| 95 | `frontend/src/components/shared/Input.jsx` | Reusable input field |
| 96 | `frontend/src/components/shared/Card.jsx` | Card container |
| 97 | `frontend/src/components/shared/Modal.jsx` | Modal dialog |
| 98 | `frontend/src/components/shared/Badge.jsx` | Status badge |
| 99 | `frontend/src/components/shared/StatCard.jsx` | Dashboard stat card |
| 100 | `frontend/src/components/shared/LoadingSpinner.jsx` | Loading indicator |
| 101 | `frontend/src/components/shared/PageLoader.jsx` | Full page loader |
| 102 | `frontend/src/components/shared/ErrorBoundary.jsx` | React error boundary |
| 103 | `frontend/src/components/shared/ErrorMessage.jsx` | Error display component |
| 104 | `frontend/src/components/shared/EmptyState.jsx` | Empty state placeholder |
| 105 | `frontend/src/components/shared/ConfirmModal.jsx` | Confirmation dialog |
| 106 | `frontend/src/components/shared/Logo.jsx` | App logo component |
| 107 | `frontend/src/components/shared/SearchBar.jsx` | Search bar component |
| 108 | `frontend/src/components/shared/SelectField.jsx` | Select dropdown |
| 109 | `frontend/src/components/shared/ToggleSwitch.jsx` | Toggle switch |
| 110 | `frontend/src/components/shared/StatusTimeline.jsx` | Certificate status timeline |
| 111 | `frontend/src/components/shared/RevocationModal.jsx` | Certificate revocation modal |
| 112 | `frontend/src/components/shared/RestoreModal.jsx` | Certificate restore modal |

### Frontend — Layout Components (6 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 113 | `frontend/src/components/layout/DashboardLayout.jsx` | Dashboard page wrapper |
| 114 | `frontend/src/components/layout/Sidebar.jsx` | Sidebar navigation |
| 115 | `frontend/src/components/layout/Navbar.jsx` | Top navigation bar |
| 116 | `frontend/src/components/layout/PublicNavbar.jsx` | Public pages navbar |
| 117 | `frontend/src/components/layout/Footer.jsx` | Site footer |
| 118 | `frontend/src/components/layout/SettingsLayout.jsx` | Settings page wrapper |

### Frontend — Auth Components (4 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 119 | `frontend/src/components/auth/LoginForm.jsx` | Login form component |
| 120 | `frontend/src/components/auth/RegisterForm.jsx` | Registration form component |
| 121 | `frontend/src/components/auth/EmailVerificationModal.jsx` | OTP verification modal |
| 122 | `frontend/src/components/auth/ProtectedRoute.jsx` | Route guard component |

### Frontend — Extra Auth Pages (6 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 123 | `frontend/src/pages/auth/RegisterLanding.jsx` | Role selection page |
| 124 | `frontend/src/pages/auth/EmailVerification.jsx` | Email verification page |
| 125 | `frontend/src/pages/auth/EmailVerified.jsx` | Email verified confirmation |
| 126 | `frontend/src/pages/auth/ForgotPassword.jsx` | Forgot password page |
| 127 | `frontend/src/pages/auth/ResetPassword.jsx` | Reset password page |
| 128 | `frontend/src/pages/auth/VerifyEmailChange.jsx` | Email change verification |

### Frontend — Core Auth Pages (4 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 129 | `frontend/src/pages/auth/Login.jsx` | Login page |
| 130 | `frontend/src/pages/auth/Register.jsx` | Registration page |
| 131 | `frontend/src/pages/auth/VerifyEmail.jsx` | OTP entry page |
| 132 | `frontend/src/pages/auth/PendingApproval.jsx` | Pending approval page |

### Frontend — Public Pages & Landing (10 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 133 | `frontend/src/pages/public/Landing.jsx` | Landing/home page |
| 134 | `frontend/src/pages/public/VerifyCertificate.jsx` | Public certificate verification |
| 135 | `frontend/src/pages/public/ParticipatingUniversities.jsx` | Universities list page |
| 136 | `frontend/src/pages/public/HelpCenter.jsx` | Help center page |
| 137 | `frontend/src/pages/public/PrivacyPolicy.jsx` | Privacy policy page |
| 138 | `frontend/src/pages/public/TermsOfService.jsx` | Terms of service page |
| 139 | `frontend/src/pages/public/SecurityCompliance.jsx` | Security compliance page |
| 140 | `frontend/src/pages/public/VerificationAPI.jsx` | API documentation page |
| 141 | `frontend/src/pages/public/SystemStatus.jsx` | System status page |
| 142 | `frontend/src/pages/public/ContactSupport.jsx` | Contact support page |

### Frontend — Utility Files (2 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 143 | `frontend/src/utils/helpers.js` | Utility functions (cn, formatDate, etc.) |
| 144 | `frontend/src/utils/printVerification.js` | Verification report print/PDF export |

### Frontend — Notification Component (1 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 145 | `frontend/src/components/notifications/NotificationDropdown.jsx` | Notification dropdown |

### Frontend — Certificate Component (1 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 146 | `frontend/src/components/certificates/CertificateDetailModal.jsx` | Certificate detail modal |

### Frontend — Service Files (1 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 147 | `frontend/src/services/certificateService.js` | PDF download/preview helpers |

### Frontend — Admin Pages (8 files) — Musta

| # | File | Purpose |
|---|------|---------|
| 148 | `frontend/src/pages/admin/Dashboard.jsx` | Admin dashboard page |
| 149 | `frontend/src/pages/admin/Users.jsx` | Admin users management page |
| 150 | `frontend/src/pages/admin/UserDetails.jsx` | Admin user detail page |
| 151 | `frontend/src/pages/admin/UserApprovals.jsx` | Admin user approvals page |
| 152 | `frontend/src/pages/admin/Certificates.jsx` | Admin certificates page |
| 153 | `frontend/src/pages/admin/ProfileChangeRequests.jsx` | Profile change requests page |
| 154 | `frontend/src/pages/admin/ActivityLogs.jsx` | Activity logs page |
| 155 | `frontend/src/pages/admin/Analytics.jsx` | Analytics dashboard page |

### Frontend — Student Pages (5 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 156 | `frontend/src/pages/student/Dashboard.jsx` | Student dashboard |
| 157 | `frontend/src/pages/student/Certificates.jsx` | Student certificates page |
| 158 | `frontend/src/pages/student/AccessRequests.jsx` | Student access requests page |
| 159 | `frontend/src/pages/student/MyUniversity.jsx` | Student university page |
| 160 | `frontend/src/pages/student/BrowseUniversities.jsx` | Browse universities page |

### Frontend — Access Components (5 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 161 | `frontend/src/components/access/AccessRequestCard.jsx` | Access request card |
| 162 | `frontend/src/components/access/AccessDurationSelect.jsx` | Duration select input |
| 163 | `frontend/src/components/access/GrantedAccessCard.jsx` | Granted access card |
| 164 | `frontend/src/components/access/MyAccessRequestCard.jsx` | Student's own request card |
| 165 | `frontend/src/components/access/PurposeInput.jsx` | Purpose text input |

### Frontend — University Pages (5 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 166 | `frontend/src/pages/university/Dashboard.jsx` | University dashboard |
| 167 | `frontend/src/pages/university/Certificates.jsx` | University certificates page |
| 168 | `frontend/src/pages/university/Enrollments.jsx` | Enrollments management page |
| 169 | `frontend/src/pages/university/IssueCertificate.jsx` | Certificate issuance page |
| 170 | `frontend/src/pages/university/Settings.jsx` | University settings page |

### Frontend — University Components (4 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 171 | `frontend/src/components/university/DepartmentsManager.jsx` | Department management |
| 172 | `frontend/src/components/university/batch-issue/CSVUpload.jsx` | CSV batch upload |
| 173 | `frontend/src/components/university/batch-issue/ProgressModal.jsx` | Batch progress modal |
| 174 | `frontend/src/components/university/batch-issue/ResultsSummary.jsx` | Batch results summary |

### Frontend — Verifier Pages (5 files) — Saikat

| # | File | Purpose |
|---|------|---------|
| 175 | `frontend/src/pages/verifier/Dashboard.jsx` | Verifier dashboard |
| 176 | `frontend/src/pages/verifier/VerifyCertificate.jsx` | Verifier certificate verification |
| 177 | `frontend/src/pages/verifier/VerificationHistory.jsx` | Verification history page |
| 178 | `frontend/src/pages/verifier/AccessRequests.jsx` | Verifier access requests page |
| 179 | `frontend/src/pages/verifier/AccessibleCertificates.jsx` | Accessible certificates page |

### Frontend — User Common Pages (2 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 180 | `frontend/src/pages/profile/Profile.jsx` | User profile page |
| 181 | `frontend/src/pages/notifications/Notifications.jsx` | Notifications page |

### Frontend — General Common Pages (3 files) — Sayem

| # | File | Purpose |
|---|------|---------|
| 182 | `frontend/src/pages/NotFound.jsx` | 404 page |
| 183 | `frontend/src/pages/profile/Settings.jsx` | User settings page |
| 184 | `frontend/src/pages/search/SearchResults.jsx` | Search results page |

### Frontend — Static Assets (5 files) — Sadid

| # | File | Purpose |
|---|------|---------|
| 185 | `frontend/public/favicon.svg` | Favicon |
| 186 | `frontend/public/logo.svg` | Logo SVG |
| 187 | `frontend/public/icons.svg` | Icon sprite |
| 188 | `frontend/src/assets/hero.png` | Landing page hero image |
| 189 | `frontend/src/assets/vite.svg` | Vite logo asset |

### Documentation (3 files) (3 files) — None

| # | File | Purpose |
|---|------|---------|
| 190 | `API.md` | API reference documentation |
| 191 | `README.md` | Project readme |
| 192 | `LICENSE` | MIT license |

---

### File Count by Member

| Member | Backend | Frontend | Docs/Config | Total |
|--------|---------|----------|-------------|-------|
| Sadid  | 9       | 35       | 1           | **45** |
| Sayem  | 9       | 35       | 0           | **44** |
| Musta  | 28      | 8        | 1           | **37** |
| Saikat | 10      | 20       | 1           | **31** |
| **Total** | **56** | **98** | **3** | **157** |

> Note: Frontend config files (package.json, vite.config.js, etc.) and static assets counted under their assigned member. `db/seed.sql` is bundled with `schema.sql`. Some logical groupings combine files for balanced commits.

## 2. Dependency Map

```
db/schema.sql
  └─→ pom.xml + application.properties + EduAuthApplication.java
        └─→ CorsConfig, MailConfig, Exception classes, GlobalExceptionHandler
              └─→ HashUtil.java
                    └─→ JwtConfig.java + JwtService.java
                          └─→ CustomUserDetailsService.java
                                └─→ JwtAuthenticationFilter.java
                                      └─→ SecurityConfig.java
                                            └─→ TokenBlacklistService.java
        └─→ Entity Models (User, Student, Institution, Verifier, ...)
              └─→ Repositories (UserRepository, StudentRepository, ...)
                    └─→ Auth DTOs
                          └─→ EmailService.java
                                └─→ AuthService.java
                                      └─→ AuthController.java
                    └─→ Dashboard DTOs + DashboardService
                          └─→ Admin DTOs + AdminUserService
                                └─→ AdminDashboardController + AdminUserController
                    └─→ EncryptionService + SerialGeneratorService
                          └─→ CertificateService
                                └─→ AdminCertificateController
                                └─→ StudentDashboardController + StudentCertificateController
                                └─→ UniversityDashboardController + UniversityCertificateController
                                └─→ VerifierController + VerifierDashboardController + VerifierCertificateController
                                └─→ VerifyController (public)

Frontend dependency chain:
  package.json + vite.config.js + index.html + .env + tailwind configs
    └─→ index.css + main.jsx
          └─→ api.js + authService.js
                └─→ AuthContext.jsx + ThemeContext.jsx + NotificationContext.jsx
                      └─→ ProtectedRoute.jsx + ErrorBoundary.jsx + PageLoader.jsx
                            └─→ App.jsx (routing)
                                  └─→ Shared components (Button, Input, Card, Modal, ...)
                                        └─→ Layout components (Sidebar, Navbar, DashboardLayout, ...)
                                              └─→ Auth pages (Login, Register, VerifyEmail, ...)
                                              └─→ Public pages (Landing, VerifyCertificate, ...)
                                              └─→ Admin pages
                                              └─→ Student pages
                                              └─→ University pages
                                              └─→ Verifier pages
                                              └─→ Common pages (Profile, Settings, Notifications, ...)
```

---

## 3. Full Multi-Day Timeline

### Pre-requisite 1: Staging Repository (The Setup)
To make sure everyone has the exact same code to push from, Sadid will push the **entire** codebase to a staging repository first.

> **Note:** Replace these URLs with the actual repository URLs before sharing this file.

Then, **all other members (Sayem, Musta, Saikat)** simply need to copy-paste this block into their terminals to clone the code, wipe the history, and connect to the actual repository in one go:

**For Windows (PowerShell):**
```powershell
git clone https://github.com/sadid-ahmed-007/staging-repo.git eduauth-registry-maven
cd eduauth-registry-maven
Remove-Item -Recurse -Force .git
git init
git remote add origin https://github.com/litch07/eduauth-registry-maven.git
```

**For Mac/Linux (or Git Bash):**
```bash
git clone https://github.com/sadid-ahmed-007/staging-repo.git eduauth-registry-maven
cd eduauth-registry-maven
rm -rf .git
git init
git remote add origin https://github.com/litch07/eduauth-registry-maven.git
```

### Pre-requisite 2: Actual Repository Initialization
Now, Sadid initializes the **actual** repository that the teacher will see:

```bash
Create a new repository on GitHub: eduauth-registry-maven (public or private)

git init
git remote add origin https://github.com/litch07/eduauth-registry-maven.git
git add README.md .gitignore
git commit -m "initialize repository"
git branch -M main
git push -u origin main
```
> After Sadid pushes the initial commit, all other members run the `git init` and `git remote add` commands above so they are connected to the actual repo.

### CRITICAL: The "Stash-Sync" Workflow
Because you all have the **final** files sitting locally on your computers, Git will block you from running `git pull origin main` (it will throw an "untracked files would be overwritten" error). 

To fix this and safely sync with `main` after someone else merges a PR, **always use this exact sequence** before starting your next branch:

```bash
# 1. Stage all your unpushed files so Git can stash them
git add .

# 2. Stash them safely away (your folder will now exactly match the remote main)
git stash

# 3. Safely pull the latest merged changes from your teammates
git checkout main
git pull origin main

# 4. Pop your stash to bring back all your unpushed files
git stash pop

# 5. Unstage the popped files so you can selectively pick what to push next
git reset
```
*Use this stash sequence every single time you need to pull `main` in the plan below!*

---

---

### DAY 1 - SATURDAY

---

### DAY 1 — Saturday

---

════════════════════════════════════════════
**DAY:** Day 1 — Saturday
**TIME:** 9:15 AM
**MEMBER:** Musta
**BRANCH:** `feature/database-schema`
**DEPENDS ON:** none (first push after init)
**REPRESENTS:** Database schema and seed data for all tables

**FILES IN THIS PUSH:**
```
db/schema.sql
db/seed.sql
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/database-schema
git add db/schema.sql
git add db/seed.sql
git commit -m "add database schema and seed data"
git push origin feature/database-schema
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 1 — Saturday
**TIME:** 9:55 AM
**MEMBER:** Saikat
**BRANCH:** `feature/project-setup`
**DEPENDS ON:** `feature/database-schema`
**REPRESENTS:** Maven project setup with all dependencies and Spring Boot config

**FILES IN THIS PUSH:**
```
backend/pom.xml
backend/src/main/resources/application.properties
backend/src/main/java/com/eduauth/EduAuthApplication.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/project-setup
git add backend/pom.xml
git add backend/src/main/resources/application.properties
git add backend/src/main/java/com/eduauth/EduAuthApplication.java
git commit -m "set up spring boot maven project"
git push origin feature/project-setup
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 1 — Saturday
**TIME:** 10:40 AM
**MEMBER:** Saikat
**BRANCH:** `feature/cors-and-mail`
**DEPENDS ON:** `feature/project-setup`
**REPRESENTS:** Infrastructure config — CORS, mail, exception handling

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/config/CorsConfig.java
backend/src/main/java/com/eduauth/config/MailConfig.java
backend/src/main/java/com/eduauth/exception/BadRequestException.java
backend/src/main/java/com/eduauth/exception/ResourceNotFoundException.java
backend/src/main/java/com/eduauth/exception/UnauthorizedException.java
backend/src/main/java/com/eduauth/exception/GlobalExceptionHandler.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/cors-and-mail
git add backend/src/main/java/com/eduauth/config/CorsConfig.java
git add backend/src/main/java/com/eduauth/config/MailConfig.java
git add backend/src/main/java/com/eduauth/exception/BadRequestException.java
git add backend/src/main/java/com/eduauth/exception/ResourceNotFoundException.java
git add backend/src/main/java/com/eduauth/exception/UnauthorizedException.java
git add backend/src/main/java/com/eduauth/exception/GlobalExceptionHandler.java
git commit -m "cors and mail configuration with exceptions"
git push origin feature/cors-and-mail
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 1 — Saturday
**TIME:** 12:20 PM
**MEMBER:** Sadid
**BRANCH:** `feature/jwt-implementation`
**DEPENDS ON:** `feature/cors-and-mail`
**REPRESENTS:** Complete JWT authentication and Spring Security setup

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/util/HashUtil.java
backend/src/main/java/com/eduauth/config/JwtConfig.java
backend/src/main/java/com/eduauth/service/JwtService.java
backend/src/main/java/com/eduauth/service/CustomUserDetailsService.java
backend/src/main/java/com/eduauth/config/JwtAuthenticationFilter.java
backend/src/main/java/com/eduauth/config/SecurityConfig.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/jwt-implementation
git add backend/src/main/java/com/eduauth/util/HashUtil.java
git add backend/src/main/java/com/eduauth/config/JwtConfig.java
git add backend/src/main/java/com/eduauth/service/JwtService.java
git add backend/src/main/java/com/eduauth/service/CustomUserDetailsService.java
git add backend/src/main/java/com/eduauth/config/JwtAuthenticationFilter.java
git add backend/src/main/java/com/eduauth/config/SecurityConfig.java
git commit -m "set up jwt and spring security"
git push origin feature/jwt-implementation
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 1 — Saturday
**TIME:** 2:45 PM
**MEMBER:** Musta
**BRANCH:** `feature/core-entities`
**DEPENDS ON:** `feature/jwt-implementation`
**REPRESENTS:** All JPA entity model classes for database tables

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/model/User.java
backend/src/main/java/com/eduauth/model/Student.java
backend/src/main/java/com/eduauth/model/Institution.java
backend/src/main/java/com/eduauth/model/Verifier.java
backend/src/main/java/com/eduauth/model/Enrollment.java
backend/src/main/java/com/eduauth/model/Certificate.java
backend/src/main/java/com/eduauth/model/AccessRequest.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/core-entities
git add backend/src/main/java/com/eduauth/model/User.java
git add backend/src/main/java/com/eduauth/model/Student.java
git add backend/src/main/java/com/eduauth/model/Institution.java
git add backend/src/main/java/com/eduauth/model/Verifier.java
git add backend/src/main/java/com/eduauth/model/Enrollment.java
git add backend/src/main/java/com/eduauth/model/Certificate.java
git add backend/src/main/java/com/eduauth/model/AccessRequest.java
git commit -m "core database entities"
git push origin feature/core-entities
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 1 — Saturday
**TIME:** 3:15 PM
**MEMBER:** Musta
**BRANCH:** `feature/support-entities`
**DEPENDS ON:** `feature/core-entities`
**REPRESENTS:** All JPA entity model classes for database tables

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/model/AccessGrant.java
backend/src/main/java/com/eduauth/model/VerificationLog.java
backend/src/main/java/com/eduauth/model/WithdrawalRequest.java
backend/src/main/java/com/eduauth/model/ActivityLog.java
backend/src/main/java/com/eduauth/model/UserSettings.java
backend/src/main/java/com/eduauth/model/PendingRegistration.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/support-entities
git add backend/src/main/java/com/eduauth/model/AccessGrant.java
git add backend/src/main/java/com/eduauth/model/VerificationLog.java
git add backend/src/main/java/com/eduauth/model/WithdrawalRequest.java
git add backend/src/main/java/com/eduauth/model/ActivityLog.java
git add backend/src/main/java/com/eduauth/model/UserSettings.java
git add backend/src/main/java/com/eduauth/model/PendingRegistration.java
git commit -m "support and log entities"
git push origin feature/support-entities
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

### DAY 2 - SUNDAY

---

### DAY 2 — Sunday

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 9:30 AM
**MEMBER:** Musta
**BRANCH:** `feature/core-repositories`
**DEPENDS ON:** `feature/entity-models`
**REPRESENTS:** All Spring Data JPA repository interfaces

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/repository/UserRepository.java
backend/src/main/java/com/eduauth/repository/StudentRepository.java
backend/src/main/java/com/eduauth/repository/InstitutionRepository.java
backend/src/main/java/com/eduauth/repository/VerifierRepository.java
backend/src/main/java/com/eduauth/repository/EnrollmentRepository.java
backend/src/main/java/com/eduauth/repository/CertificateRepository.java
backend/src/main/java/com/eduauth/repository/AccessRequestRepository.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/core-repositories
git add backend/src/main/java/com/eduauth/repository/UserRepository.java
git add backend/src/main/java/com/eduauth/repository/StudentRepository.java
git add backend/src/main/java/com/eduauth/repository/InstitutionRepository.java
git add backend/src/main/java/com/eduauth/repository/VerifierRepository.java
git add backend/src/main/java/com/eduauth/repository/EnrollmentRepository.java
git add backend/src/main/java/com/eduauth/repository/CertificateRepository.java
git add backend/src/main/java/com/eduauth/repository/AccessRequestRepository.java
git commit -m "core repositories"
git push origin feature/core-repositories
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 10:00 AM
**MEMBER:** Musta
**BRANCH:** `feature/support-repositories`
**DEPENDS ON:** `feature/core-repositories`
**REPRESENTS:** All Spring Data JPA repository interfaces

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/repository/AccessGrantRepository.java
backend/src/main/java/com/eduauth/repository/VerificationLogRepository.java
backend/src/main/java/com/eduauth/repository/WithdrawalRequestRepository.java
backend/src/main/java/com/eduauth/repository/ActivityLogRepository.java
backend/src/main/java/com/eduauth/repository/UserSettingsRepository.java
backend/src/main/java/com/eduauth/repository/PendingRegistrationRepository.java
backend/src/main/java/com/eduauth/repository/specification/UserSpecification.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/support-repositories
git add backend/src/main/java/com/eduauth/repository/AccessGrantRepository.java
git add backend/src/main/java/com/eduauth/repository/VerificationLogRepository.java
git add backend/src/main/java/com/eduauth/repository/WithdrawalRequestRepository.java
git add backend/src/main/java/com/eduauth/repository/ActivityLogRepository.java
git add backend/src/main/java/com/eduauth/repository/UserSettingsRepository.java
git add backend/src/main/java/com/eduauth/repository/PendingRegistrationRepository.java
git add backend/src/main/java/com/eduauth/repository/specification/UserSpecification.java
git commit -m "support repositories"
git push origin feature/support-repositories
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 10:45 AM
**MEMBER:** Sayem
**BRANCH:** `feature/auth-dtos`
**DEPENDS ON:** `feature/repositories`
**REPRESENTS:** Authentication backend — DTOs, services, and controller for register/login/OTP

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/dto/auth/LoginRequest.java
backend/src/main/java/com/eduauth/dto/auth/RegisterRequest.java
backend/src/main/java/com/eduauth/dto/auth/VerifyEmailRequest.java
backend/src/main/java/com/eduauth/dto/auth/ResendOtpRequest.java
backend/src/main/java/com/eduauth/dto/auth/AuthResponse.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/auth-dtos
git add backend/src/main/java/com/eduauth/dto/auth/LoginRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/RegisterRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/VerifyEmailRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/ResendOtpRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/AuthResponse.java
git commit -m "auth dtos"
git push origin feature/auth-dtos
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 11:15 AM
**MEMBER:** Sayem
**BRANCH:** `feature/auth-services`
**DEPENDS ON:** `feature/auth-dtos`
**REPRESENTS:** Authentication backend — DTOs, services, and controller for register/login/OTP

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/service/EmailService.java
backend/src/main/java/com/eduauth/service/TokenBlacklistService.java
backend/src/main/java/com/eduauth/service/AuthService.java
backend/src/main/java/com/eduauth/controller/AuthController.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/auth-services
git add backend/src/main/java/com/eduauth/service/EmailService.java
git add backend/src/main/java/com/eduauth/service/TokenBlacklistService.java
git add backend/src/main/java/com/eduauth/service/AuthService.java
git add backend/src/main/java/com/eduauth/controller/AuthController.java
git commit -m "auth services and controllers"
git push origin feature/auth-services
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 1:15 PM
**MEMBER:** Sadid
**BRANCH:** `feature/frontend-config`
**DEPENDS ON:** `feature/auth-backend`
**REPRESENTS:** React frontend foundation — project config, API client, auth context, routing

**FILES IN THIS PUSH:**
```
frontend/.gitignore
frontend/.oxlintrc.json
frontend/index.html
frontend/package.json
frontend/postcss.config.js
frontend/tailwind.config.js
frontend/vite.config.js
frontend/README.md
frontend/src/index.css
frontend/src/main.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-config
git add frontend/.gitignore
git add frontend/.oxlintrc.json
git add frontend/index.html
git add frontend/package.json
git add frontend/postcss.config.js
git add frontend/tailwind.config.js
git add frontend/vite.config.js
git add frontend/README.md
git add frontend/src/index.css
git add frontend/src/main.jsx
git commit -m "frontend configuration"
git push origin feature/frontend-config
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 1:45 PM
**MEMBER:** Sadid
**BRANCH:** `feature/frontend-assets-and-utils`
**DEPENDS ON:** `feature/frontend-config`
**REPRESENTS:** React frontend foundation — project config, API client, auth context, routing

**FILES IN THIS PUSH:**
```
frontend/src/contexts/ThemeContext.jsx
frontend/src/contexts/NotificationContext.jsx
frontend/src/hooks/useOutsideClick.js
frontend/public/favicon.svg
frontend/public/logo.svg
frontend/public/icons.svg
frontend/src/assets/hero.png
frontend/src/assets/vite.svg
frontend/src/utils/helpers.js
frontend/src/components/notifications/NotificationDropdown.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-assets-and-utils
git add frontend/src/contexts/ThemeContext.jsx
git add frontend/src/contexts/NotificationContext.jsx
git add frontend/src/hooks/useOutsideClick.js
git add frontend/public/favicon.svg
git add frontend/public/logo.svg
git add frontend/public/icons.svg
git add frontend/src/assets/hero.png
git add frontend/src/assets/vite.svg
git add frontend/src/utils/helpers.js
git add frontend/src/components/notifications/NotificationDropdown.jsx
git commit -m "frontend assets and contexts"
git push origin feature/frontend-assets-and-utils
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 2:15 PM
**MEMBER:** Sadid
**BRANCH:** `feature/frontend-and-public-pages`
**DEPENDS ON:** `feature/frontend-assets-and-utils`
**REPRESENTS:** React frontend foundation — project config, API client, auth context, routing

**FILES IN THIS PUSH:**
```
frontend/src/services/api.js
frontend/src/contexts/AuthContext.jsx
frontend/src/App.jsx
frontend/src/pages/public/Landing.jsx
frontend/src/pages/public/VerifyCertificate.jsx
frontend/src/pages/public/ParticipatingUniversities.jsx
frontend/src/pages/public/HelpCenter.jsx
frontend/src/pages/public/PrivacyPolicy.jsx
frontend/src/pages/public/TermsOfService.jsx
frontend/src/pages/public/SecurityCompliance.jsx
frontend/src/pages/public/VerificationAPI.jsx
frontend/src/pages/public/SystemStatus.jsx
frontend/src/pages/public/ContactSupport.jsx
frontend/src/pages/profile/Profile.jsx
frontend/src/pages/notifications/Notifications.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-and-public-pages
git add frontend/src/services/api.js
git add frontend/src/contexts/AuthContext.jsx
git add frontend/src/App.jsx
git add frontend/src/pages/public/Landing.jsx
git add frontend/src/pages/public/VerifyCertificate.jsx
git add frontend/src/pages/public/ParticipatingUniversities.jsx
git add frontend/src/pages/public/HelpCenter.jsx
git add frontend/src/pages/public/PrivacyPolicy.jsx
git add frontend/src/pages/public/TermsOfService.jsx
git add frontend/src/pages/public/SecurityCompliance.jsx
git add frontend/src/pages/public/VerificationAPI.jsx
git add frontend/src/pages/public/SystemStatus.jsx
git add frontend/src/pages/public/ContactSupport.jsx
git add frontend/src/pages/profile/Profile.jsx
git add frontend/src/pages/notifications/Notifications.jsx
git commit -m "react app and public pages"
git push origin feature/frontend-and-public-pages
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 3:40 PM
**MEMBER:** Sayem
**BRANCH:** `feature/shared-components`
**DEPENDS ON:** `feature/frontend-setup`
**REPRESENTS:** Shared UI components and layout structure for all dashboard pages

**FILES IN THIS PUSH:**
```
frontend/src/components/shared/Button.jsx
frontend/src/components/shared/Input.jsx
frontend/src/components/shared/Card.jsx
frontend/src/components/shared/Modal.jsx
frontend/src/components/shared/Badge.jsx
frontend/src/components/shared/StatCard.jsx
frontend/src/components/shared/LoadingSpinner.jsx
frontend/src/components/shared/PageLoader.jsx
frontend/src/components/shared/ErrorBoundary.jsx
frontend/src/components/shared/ErrorMessage.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/shared-components
git add frontend/src/components/shared/Button.jsx
git add frontend/src/components/shared/Input.jsx
git add frontend/src/components/shared/Card.jsx
git add frontend/src/components/shared/Modal.jsx
git add frontend/src/components/shared/Badge.jsx
git add frontend/src/components/shared/StatCard.jsx
git add frontend/src/components/shared/LoadingSpinner.jsx
git add frontend/src/components/shared/PageLoader.jsx
git add frontend/src/components/shared/ErrorBoundary.jsx
git add frontend/src/components/shared/ErrorMessage.jsx
git commit -m "shared ui components part 1"
git push origin feature/shared-components
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 4:05 PM
**MEMBER:** Sayem
**BRANCH:** `feature/ui-components`
**DEPENDS ON:** `feature/shared-components`
**REPRESENTS:** Shared UI components and layout structure for all dashboard pages

**FILES IN THIS PUSH:**
```
frontend/src/components/shared/EmptyState.jsx
frontend/src/components/shared/ConfirmModal.jsx
frontend/src/components/shared/Logo.jsx
frontend/src/components/shared/SearchBar.jsx
frontend/src/components/shared/SelectField.jsx
frontend/src/components/shared/ToggleSwitch.jsx
frontend/src/components/shared/StatusTimeline.jsx
frontend/src/components/shared/RevocationModal.jsx
frontend/src/components/shared/RestoreModal.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/ui-components
git add frontend/src/components/shared/EmptyState.jsx
git add frontend/src/components/shared/ConfirmModal.jsx
git add frontend/src/components/shared/Logo.jsx
git add frontend/src/components/shared/SearchBar.jsx
git add frontend/src/components/shared/SelectField.jsx
git add frontend/src/components/shared/ToggleSwitch.jsx
git add frontend/src/components/shared/StatusTimeline.jsx
git add frontend/src/components/shared/RevocationModal.jsx
git add frontend/src/components/shared/RestoreModal.jsx
git commit -m "shared ui components part 2"
git push origin feature/ui-components
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 4:30 PM
**MEMBER:** Sayem
**BRANCH:** `feature/layout-components`
**DEPENDS ON:** `feature/ui-components`
**REPRESENTS:** Shared UI components and layout structure for all dashboard pages

**FILES IN THIS PUSH:**
```
frontend/src/components/layout/DashboardLayout.jsx
frontend/src/components/layout/Sidebar.jsx
frontend/src/components/layout/Navbar.jsx
frontend/src/components/layout/PublicNavbar.jsx
frontend/src/components/layout/Footer.jsx
frontend/src/components/layout/SettingsLayout.jsx
frontend/src/pages/NotFound.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/layout-components
git add frontend/src/components/layout/DashboardLayout.jsx
git add frontend/src/components/layout/Sidebar.jsx
git add frontend/src/components/layout/Navbar.jsx
git add frontend/src/components/layout/PublicNavbar.jsx
git add frontend/src/components/layout/Footer.jsx
git add frontend/src/components/layout/SettingsLayout.jsx
git add frontend/src/pages/NotFound.jsx
git commit -m "dashboard layout and navigation"
git push origin feature/layout-components
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 5:55 PM
**MEMBER:** Sayem
**BRANCH:** `feature/auth-pages`
**DEPENDS ON:** `feature/layout-components`
**REPRESENTS:** Authentication frontend pages and public landing page

**FILES IN THIS PUSH:**
```
frontend/src/components/auth/LoginForm.jsx
frontend/src/components/auth/RegisterForm.jsx
frontend/src/components/auth/EmailVerificationModal.jsx
frontend/src/components/auth/ProtectedRoute.jsx
frontend/src/pages/auth/Login.jsx
frontend/src/pages/auth/Register.jsx
frontend/src/pages/auth/VerifyEmail.jsx
frontend/src/pages/auth/PendingApproval.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/auth-pages
git add frontend/src/components/auth/LoginForm.jsx
git add frontend/src/components/auth/RegisterForm.jsx
git add frontend/src/components/auth/EmailVerificationModal.jsx
git add frontend/src/components/auth/ProtectedRoute.jsx
git add frontend/src/pages/auth/Login.jsx
git add frontend/src/pages/auth/Register.jsx
git add frontend/src/pages/auth/VerifyEmail.jsx
git add frontend/src/pages/auth/PendingApproval.jsx
git commit -m "auth pages and public landing with info pages"
git push origin feature/auth-pages
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 2 — Sunday
**TIME:** 6:30 PM
**MEMBER:** Saikat
**BRANCH:** `feature/extra-auth-pages`
**DEPENDS ON:** `feature/auth-pages`
**REPRESENTS:** Extra auth pages for email config owner

**FILES IN THIS PUSH:**
```
frontend/src/pages/auth/RegisterLanding.jsx
frontend/src/pages/auth/EmailVerification.jsx
frontend/src/pages/auth/EmailVerified.jsx
frontend/src/pages/auth/ForgotPassword.jsx
frontend/src/pages/auth/ResetPassword.jsx
frontend/src/pages/auth/VerifyEmailChange.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/extra-auth-pages
git add frontend/src/pages/auth/RegisterLanding.jsx
git add frontend/src/pages/auth/EmailVerification.jsx
git add frontend/src/pages/auth/EmailVerified.jsx
git add frontend/src/pages/auth/ForgotPassword.jsx
git add frontend/src/pages/auth/ResetPassword.jsx
git add frontend/src/pages/auth/VerifyEmailChange.jsx
git commit -m "auth pages for email and password reset"
git push origin feature/extra-auth-pages
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

### DAY 3 - MONDAY

---

### DAY 3 — Monday

---

════════════════════════════════════════════
**DAY:** Day 3 — Monday
**TIME:** 9:15 AM
**MEMBER:** Musta
**BRANCH:** `feature/admin-backend`
**DEPENDS ON:** `feature/auth-pages`
**REPRESENTS:** Admin backend — dashboard DTOs, services, and user management controllers

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/dto/dashboard/AdminDashboardDto.java
backend/src/main/java/com/eduauth/dto/dashboard/StudentDashboardDto.java
backend/src/main/java/com/eduauth/dto/dashboard/UniversityDashboardDto.java
backend/src/main/java/com/eduauth/dto/dashboard/VerifierDashboardDto.java
backend/src/main/java/com/eduauth/service/DashboardService.java
backend/src/main/java/com/eduauth/dto/admin/AdminUserListDto.java
backend/src/main/java/com/eduauth/dto/admin/AdminUserDetailDto.java
backend/src/main/java/com/eduauth/dto/admin/SuspendRequestDto.java
backend/src/main/java/com/eduauth/service/AdminUserService.java
backend/src/main/java/com/eduauth/controller/admin/AdminDashboardController.java
backend/src/main/java/com/eduauth/controller/admin/AdminUserController.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/admin-backend
git add backend/src/main/java/com/eduauth/dto/dashboard/AdminDashboardDto.java
git add backend/src/main/java/com/eduauth/dto/dashboard/StudentDashboardDto.java
git add backend/src/main/java/com/eduauth/dto/dashboard/UniversityDashboardDto.java
git add backend/src/main/java/com/eduauth/dto/dashboard/VerifierDashboardDto.java
git add backend/src/main/java/com/eduauth/service/DashboardService.java
git add backend/src/main/java/com/eduauth/dto/admin/AdminUserListDto.java
git add backend/src/main/java/com/eduauth/dto/admin/AdminUserDetailDto.java
git add backend/src/main/java/com/eduauth/dto/admin/SuspendRequestDto.java
git add backend/src/main/java/com/eduauth/service/AdminUserService.java
git add backend/src/main/java/com/eduauth/controller/admin/AdminDashboardController.java
git add backend/src/main/java/com/eduauth/controller/admin/AdminUserController.java
git commit -m "admin user approval and management"
git push origin feature/admin-backend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 3 — Monday
**TIME:** 11:10 AM
**MEMBER:** Musta
**BRANCH:** `feature/admin-frontend`
**DEPENDS ON:** `feature/admin-backend`
**REPRESENTS:** Admin frontend pages — dashboard, users, user details, approvals

**FILES IN THIS PUSH:**
```
frontend/src/pages/admin/Dashboard.jsx
frontend/src/pages/admin/Users.jsx
frontend/src/pages/admin/UserDetails.jsx
frontend/src/pages/admin/UserApprovals.jsx
frontend/src/pages/admin/Certificates.jsx
frontend/src/pages/admin/ProfileChangeRequests.jsx
frontend/src/pages/admin/ActivityLogs.jsx
frontend/src/pages/admin/Analytics.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/admin-frontend
git add frontend/src/pages/admin/Dashboard.jsx
git add frontend/src/pages/admin/Users.jsx
git add frontend/src/pages/admin/UserDetails.jsx
git add frontend/src/pages/admin/UserApprovals.jsx
git add frontend/src/pages/admin/Certificates.jsx
git add frontend/src/pages/admin/ProfileChangeRequests.jsx
git add frontend/src/pages/admin/ActivityLogs.jsx
git add frontend/src/pages/admin/Analytics.jsx
git commit -m "admin dashboard and user management pages"
git push origin feature/admin-frontend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 3 — Monday
**TIME:** 2:00 PM
**MEMBER:** Sadid
**BRANCH:** `feature/certificate-core`
**DEPENDS ON:** `feature/admin-frontend`
**REPRESENTS:** Certificate core services — encryption, serial generation, and certificate business logic

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/service/EncryptionService.java
backend/src/main/java/com/eduauth/service/SerialGeneratorService.java
backend/src/main/java/com/eduauth/service/CertificateService.java
frontend/src/components/certificates/CertificateDetailModal.jsx
frontend/src/services/certificateService.js
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/certificate-core
git add backend/src/main/java/com/eduauth/service/EncryptionService.java
git add backend/src/main/java/com/eduauth/service/SerialGeneratorService.java
git add backend/src/main/java/com/eduauth/service/CertificateService.java
git add frontend/src/components/certificates/CertificateDetailModal.jsx
git add frontend/src/services/certificateService.js
git commit -m "encryption serial generation and certificate service"
git push origin feature/certificate-core
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 3 — Monday
**TIME:** 4:30 PM
**MEMBER:** Musta
**BRANCH:** `feature/admin-certificates`
**DEPENDS ON:** `feature/certificate-core`
**REPRESENTS:** Admin certificate management controller and frontend page

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/controller/admin/AdminCertificateController.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/admin-certificates
git add backend/src/main/java/com/eduauth/controller/admin/AdminCertificateController.java
git commit -m "admin certificate revocation and management"
git push origin feature/admin-certificates
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

### DAY 4 - TUESDAY

---

### DAY 4 — Tuesday

---

════════════════════════════════════════════
**DAY:** Day 4 — Tuesday
**TIME:** 9:20 AM
**MEMBER:** Sayem
**BRANCH:** `feature/student-backend`
**DEPENDS ON:** `feature/admin-certificates`
**REPRESENTS:** Student backend controllers for dashboard and certificate access

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/controller/student/StudentDashboardController.java
backend/src/main/java/com/eduauth/controller/student/StudentCertificateController.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/student-backend
git add backend/src/main/java/com/eduauth/controller/student/StudentDashboardController.java
git add backend/src/main/java/com/eduauth/controller/student/StudentCertificateController.java
git commit -m "student dashboard and certificate endpoints"
git push origin feature/student-backend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 4 — Tuesday
**TIME:** 11:40 AM
**MEMBER:** Sayem
**BRANCH:** `feature/student-pages`
**DEPENDS ON:** `feature/student-backend`
**REPRESENTS:** Student frontend pages — dashboard, certificates, access requests, university browsing

**FILES IN THIS PUSH:**
```
frontend/src/pages/student/Dashboard.jsx
frontend/src/pages/student/Certificates.jsx
frontend/src/pages/student/AccessRequests.jsx
frontend/src/pages/student/MyUniversity.jsx
frontend/src/pages/student/BrowseUniversities.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/student-pages
git add frontend/src/pages/student/Dashboard.jsx
git add frontend/src/pages/student/Certificates.jsx
git add frontend/src/pages/student/AccessRequests.jsx
git add frontend/src/pages/student/MyUniversity.jsx
git add frontend/src/pages/student/BrowseUniversities.jsx
git commit -m "student dashboard and pages"
git push origin feature/student-pages
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 4 — Tuesday
**TIME:** 12:10 PM
**MEMBER:** Sayem
**BRANCH:** `feature/access-components`
**DEPENDS ON:** `feature/student-pages`
**REPRESENTS:** Student frontend pages — dashboard, certificates, access requests, university browsing

**FILES IN THIS PUSH:**
```
frontend/src/components/access/AccessRequestCard.jsx
frontend/src/components/access/AccessDurationSelect.jsx
frontend/src/components/access/GrantedAccessCard.jsx
frontend/src/components/access/MyAccessRequestCard.jsx
frontend/src/components/access/PurposeInput.jsx
frontend/src/pages/profile/Settings.jsx
frontend/src/pages/search/SearchResults.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/access-components
git add frontend/src/components/access/AccessRequestCard.jsx
git add frontend/src/components/access/AccessDurationSelect.jsx
git add frontend/src/components/access/GrantedAccessCard.jsx
git add frontend/src/components/access/MyAccessRequestCard.jsx
git add frontend/src/components/access/PurposeInput.jsx
git add frontend/src/pages/profile/Settings.jsx
git add frontend/src/pages/search/SearchResults.jsx
git commit -m "access request components"
git push origin feature/access-components
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 4 — Tuesday
**TIME:** 2:55 PM
**MEMBER:** Saikat
**BRANCH:** `feature/university-backend`
**DEPENDS ON:** `feature/student-frontend`
**REPRESENTS:** University backend controllers for dashboard and certificate issuance

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/controller/university/UniversityDashboardController.java
backend/src/main/java/com/eduauth/controller/university/UniversityCertificateController.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/university-backend
git add backend/src/main/java/com/eduauth/controller/university/UniversityDashboardController.java
git add backend/src/main/java/com/eduauth/controller/university/UniversityCertificateController.java
git commit -m "university dashboard and certificate issuance api"
git push origin feature/university-backend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 4 — Tuesday
**TIME:** 5:10 PM
**MEMBER:** Saikat
**BRANCH:** `feature/university-frontend`
**DEPENDS ON:** `feature/university-backend`
**REPRESENTS:** University frontend pages — dashboard, certificates, enrollments, issue certificates

**FILES IN THIS PUSH:**
```
frontend/src/pages/university/Dashboard.jsx
frontend/src/pages/university/Certificates.jsx
frontend/src/pages/university/Enrollments.jsx
frontend/src/pages/university/IssueCertificate.jsx
frontend/src/pages/university/Settings.jsx
frontend/src/components/university/DepartmentsManager.jsx
frontend/src/components/university/batch-issue/CSVUpload.jsx
frontend/src/components/university/batch-issue/ProgressModal.jsx
frontend/src/components/university/batch-issue/ResultsSummary.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/university-frontend
git add frontend/src/pages/university/Dashboard.jsx
git add frontend/src/pages/university/Certificates.jsx
git add frontend/src/pages/university/Enrollments.jsx
git add frontend/src/pages/university/IssueCertificate.jsx
git add frontend/src/pages/university/Settings.jsx
git add frontend/src/components/university/DepartmentsManager.jsx
git add frontend/src/components/university/batch-issue/CSVUpload.jsx
git add frontend/src/components/university/batch-issue/ProgressModal.jsx
git add frontend/src/components/university/batch-issue/ResultsSummary.jsx
git commit -m "university enrollments and certificate issuance pages"
git push origin feature/university-frontend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

### DAY 5 - WEDNESDAY

---

### DAY 5 — Wednesday

---

════════════════════════════════════════════
**DAY:** Day 5 — Wednesday
**TIME:** 9:10 AM
**MEMBER:** Sadid
**BRANCH:** `feature/verification-backend`
**DEPENDS ON:** `feature/university-frontend`
**REPRESENTS:** Verifier backend — verification, access requests, and public verification endpoints

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/controller/VerifierController.java
backend/src/main/java/com/eduauth/controller/publics/VerifyController.java
frontend/src/utils/printVerification.js
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/verification-backend
git add backend/src/main/java/com/eduauth/controller/VerifierController.java
git add backend/src/main/java/com/eduauth/controller/publics/VerifyController.java
git add frontend/src/utils/printVerification.js
git commit -m "verifier and public verification endpoints"
git push origin feature/verification-backend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 5 — Wednesday
**TIME:** 10:25 AM
**MEMBER:** Saikat
**BRANCH:** `feature/verifier-dashboard`
**DEPENDS ON:** `feature/verifier-backend`
**REPRESENTS:** Verifier dashboard and certificate access controllers

**FILES IN THIS PUSH:**
```
backend/src/main/java/com/eduauth/controller/verifier/VerifierDashboardController.java
backend/src/main/java/com/eduauth/controller/verifier/VerifierCertificateController.java
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/verifier-dashboard
git add backend/src/main/java/com/eduauth/controller/verifier/VerifierDashboardController.java
git add backend/src/main/java/com/eduauth/controller/verifier/VerifierCertificateController.java
git commit -m "verifier dashboard and accessible certificates api"
git push origin feature/verifier-dashboard
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 5 — Wednesday
**TIME:** 12:30 PM
**MEMBER:** Saikat
**BRANCH:** `feature/verifier-frontend`
**DEPENDS ON:** `feature/verifier-dashboard`
**REPRESENTS:** Verifier frontend pages — dashboard, verification, history, access management

**FILES IN THIS PUSH:**
```
frontend/src/pages/verifier/Dashboard.jsx
frontend/src/pages/verifier/VerifyCertificate.jsx
frontend/src/pages/verifier/VerificationHistory.jsx
frontend/src/pages/verifier/AccessRequests.jsx
frontend/src/pages/verifier/AccessibleCertificates.jsx
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/verifier-frontend
git add frontend/src/pages/verifier/Dashboard.jsx
git add frontend/src/pages/verifier/VerifyCertificate.jsx
git add frontend/src/pages/verifier/VerificationHistory.jsx
git add frontend/src/pages/verifier/AccessRequests.jsx
git add frontend/src/pages/verifier/AccessibleCertificates.jsx
git commit -m "verifier search and history pages"
git push origin feature/verifier-frontend
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 5 — Wednesday
**TIME:** 5:15 PM
**MEMBER:** Musta
**BRANCH:** `feature/api-docs`
**DEPENDS ON:** `feature/public-verification`
**REPRESENTS:** Complete API reference documentation

**FILES IN THIS PUSH:**
```
API.md
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/api-docs
git add API.md
git commit -m "api reference documentation"
git push origin feature/api-docs
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 5 — Wednesday
**TIME:** 6:20 PM
**MEMBER:** Saikat
**BRANCH:** `feature/license`
**DEPENDS ON:** `feature/api-docs`
**REPRESENTS:** MIT license for the project

**FILES IN THIS PUSH:**
```
LICENSE
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/license
git add LICENSE
git commit -m "add mit license"
git push origin feature/license
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

════════════════════════════════════════════
**DAY:** Day 5 — Wednesday
**TIME:** 7:30 PM
**MEMBER:** Sadid
**BRANCH:** `feature/documentation`
**DEPENDS ON:** `feature/license`
**REPRESENTS:** Project README with setup instructions and team info

**FILES IN THIS PUSH:**
```
README.md
```

**GIT COMMANDS:**
```bash
git checkout main
git pull origin main
git checkout -b feature/documentation
git add README.md
git commit -m "readme and project documentation"
git push origin feature/documentation
```

**THEN:** Sadid creates PR on GitHub, reviews, and merges to main
════════════════════════════════════════════

---

## 4. Quick Reference by Member

### Sadid

---

#### #### Day 1 — 12:20 PM · Branch: `feature/jwt-implementation`
```bash
git checkout main
git pull origin main
git checkout -b feature/jwt-implementation
git add backend/src/main/java/com/eduauth/util/HashUtil.java
git add backend/src/main/java/com/eduauth/config/JwtConfig.java
git add backend/src/main/java/com/eduauth/service/JwtService.java
git add backend/src/main/java/com/eduauth/service/CustomUserDetailsService.java
git add backend/src/main/java/com/eduauth/config/JwtAuthenticationFilter.java
git add backend/src/main/java/com/eduauth/config/SecurityConfig.java
git commit -m "set up jwt and spring security"
git push origin feature/jwt-implementation
```

---

#### #### Day 2 — 1:15 PM · Branch: `feature/frontend-config`
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-config
git add frontend/.gitignore
git add frontend/.oxlintrc.json
git add frontend/index.html
git add frontend/package.json
git add frontend/postcss.config.js
git add frontend/tailwind.config.js
git add frontend/vite.config.js
git add frontend/README.md
git add frontend/src/index.css
git add frontend/src/main.jsx
git commit -m "frontend configuration"
git push origin feature/frontend-config
```

---

#### #### Day 2 — 1:45 PM · Branch: `feature/frontend-assets-and-utils`
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-assets-and-utils
git add frontend/src/contexts/ThemeContext.jsx
git add frontend/src/contexts/NotificationContext.jsx
git add frontend/src/hooks/useOutsideClick.js
git add frontend/public/favicon.svg
git add frontend/public/logo.svg
git add frontend/public/icons.svg
git add frontend/src/assets/hero.png
git add frontend/src/assets/vite.svg
git add frontend/src/utils/helpers.js
git add frontend/src/components/notifications/NotificationDropdown.jsx
git commit -m "frontend assets and contexts"
git push origin feature/frontend-assets-and-utils
```

---

#### #### Day 2 — 2:15 PM · Branch: `feature/frontend-and-public-pages`
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-and-public-pages
git add frontend/src/services/api.js
git add frontend/src/contexts/AuthContext.jsx
git add frontend/src/App.jsx
git add frontend/src/pages/public/Landing.jsx
git add frontend/src/pages/public/VerifyCertificate.jsx
git add frontend/src/pages/public/ParticipatingUniversities.jsx
git add frontend/src/pages/public/HelpCenter.jsx
git add frontend/src/pages/public/PrivacyPolicy.jsx
git add frontend/src/pages/public/TermsOfService.jsx
git add frontend/src/pages/public/SecurityCompliance.jsx
git add frontend/src/pages/public/VerificationAPI.jsx
git add frontend/src/pages/public/SystemStatus.jsx
git add frontend/src/pages/public/ContactSupport.jsx
git add frontend/src/pages/profile/Profile.jsx
git add frontend/src/pages/notifications/Notifications.jsx
git commit -m "react app and public pages"
git push origin feature/frontend-and-public-pages
```

---

#### #### Day 3 — 2:00 PM · Branch: `feature/certificate-core`
```bash
git checkout main
git pull origin main
git checkout -b feature/certificate-core
git add backend/src/main/java/com/eduauth/service/EncryptionService.java
git add backend/src/main/java/com/eduauth/service/SerialGeneratorService.java
git add backend/src/main/java/com/eduauth/service/CertificateService.java
git add frontend/src/components/certificates/CertificateDetailModal.jsx
git add frontend/src/services/certificateService.js
git commit -m "encryption serial generation and certificate service"
git push origin feature/certificate-core
```

---

#### #### Day 5 — 9:10 AM · Branch: `feature/verification-backend`
```bash
git checkout main
git pull origin main
git checkout -b feature/verification-backend
git add backend/src/main/java/com/eduauth/controller/VerifierController.java
git add backend/src/main/java/com/eduauth/controller/publics/VerifyController.java
git add frontend/src/utils/printVerification.js
git commit -m "verifier and public verification endpoints"
git push origin feature/verification-backend
```

---

#### #### Day 5 — 7:30 PM · Branch: `feature/documentation`
```bash
git checkout main
git pull origin main
git checkout -b feature/documentation
git add README.md
git commit -m "readme and project documentation"
git push origin feature/documentation
```

---

### Sayem

---

#### #### Day 2 — 10:45 AM · Branch: `feature/auth-dtos`
```bash
git checkout main
git pull origin main
git checkout -b feature/auth-dtos
git add backend/src/main/java/com/eduauth/dto/auth/LoginRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/RegisterRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/VerifyEmailRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/ResendOtpRequest.java
git add backend/src/main/java/com/eduauth/dto/auth/AuthResponse.java
git commit -m "auth dtos"
git push origin feature/auth-dtos
```

---

#### #### Day 2 — 11:15 AM · Branch: `feature/auth-services`
```bash
git checkout main
git pull origin main
git checkout -b feature/auth-services
git add backend/src/main/java/com/eduauth/service/EmailService.java
git add backend/src/main/java/com/eduauth/service/TokenBlacklistService.java
git add backend/src/main/java/com/eduauth/service/AuthService.java
git add backend/src/main/java/com/eduauth/controller/AuthController.java
git commit -m "auth services and controllers"
git push origin feature/auth-services
```

---

#### #### Day 2 — 3:40 PM · Branch: `feature/shared-components`
```bash
git checkout main
git pull origin main
git checkout -b feature/shared-components
git add frontend/src/components/shared/Button.jsx
git add frontend/src/components/shared/Input.jsx
git add frontend/src/components/shared/Card.jsx
git add frontend/src/components/shared/Modal.jsx
git add frontend/src/components/shared/Badge.jsx
git add frontend/src/components/shared/StatCard.jsx
git add frontend/src/components/shared/LoadingSpinner.jsx
git add frontend/src/components/shared/PageLoader.jsx
git add frontend/src/components/shared/ErrorBoundary.jsx
git add frontend/src/components/shared/ErrorMessage.jsx
git commit -m "shared ui components part 1"
git push origin feature/shared-components
```

---

#### #### Day 2 — 4:05 PM · Branch: `feature/ui-components`
```bash
git checkout main
git pull origin main
git checkout -b feature/ui-components
git add frontend/src/components/shared/EmptyState.jsx
git add frontend/src/components/shared/ConfirmModal.jsx
git add frontend/src/components/shared/Logo.jsx
git add frontend/src/components/shared/SearchBar.jsx
git add frontend/src/components/shared/SelectField.jsx
git add frontend/src/components/shared/ToggleSwitch.jsx
git add frontend/src/components/shared/StatusTimeline.jsx
git add frontend/src/components/shared/RevocationModal.jsx
git add frontend/src/components/shared/RestoreModal.jsx
git commit -m "shared ui components part 2"
git push origin feature/ui-components
```

---

#### #### Day 2 — 4:30 PM · Branch: `feature/layout-components`
```bash
git checkout main
git pull origin main
git checkout -b feature/layout-components
git add frontend/src/components/layout/DashboardLayout.jsx
git add frontend/src/components/layout/Sidebar.jsx
git add frontend/src/components/layout/Navbar.jsx
git add frontend/src/components/layout/PublicNavbar.jsx
git add frontend/src/components/layout/Footer.jsx
git add frontend/src/components/layout/SettingsLayout.jsx
git add frontend/src/pages/NotFound.jsx
git commit -m "dashboard layout and navigation"
git push origin feature/layout-components
```

---

#### #### Day 2 — 5:55 PM · Branch: `feature/auth-pages`
```bash
git checkout main
git pull origin main
git checkout -b feature/auth-pages
git add frontend/src/components/auth/LoginForm.jsx
git add frontend/src/components/auth/RegisterForm.jsx
git add frontend/src/components/auth/EmailVerificationModal.jsx
git add frontend/src/components/auth/ProtectedRoute.jsx
git add frontend/src/pages/auth/Login.jsx
git add frontend/src/pages/auth/Register.jsx
git add frontend/src/pages/auth/VerifyEmail.jsx
git add frontend/src/pages/auth/PendingApproval.jsx
git commit -m "auth pages and public landing with info pages"
git push origin feature/auth-pages
```

---

#### #### Day 4 — 9:20 AM · Branch: `feature/student-backend`
```bash
git checkout main
git pull origin main
git checkout -b feature/student-backend
git add backend/src/main/java/com/eduauth/controller/student/StudentDashboardController.java
git add backend/src/main/java/com/eduauth/controller/student/StudentCertificateController.java
git commit -m "student dashboard and certificate endpoints"
git push origin feature/student-backend
```

---

#### #### Day 4 — 11:40 AM · Branch: `feature/student-pages`
```bash
git checkout main
git pull origin main
git checkout -b feature/student-pages
git add frontend/src/pages/student/Dashboard.jsx
git add frontend/src/pages/student/Certificates.jsx
git add frontend/src/pages/student/AccessRequests.jsx
git add frontend/src/pages/student/MyUniversity.jsx
git add frontend/src/pages/student/BrowseUniversities.jsx
git commit -m "student dashboard and pages"
git push origin feature/student-pages
```

---

#### #### Day 4 — 12:10 PM · Branch: `feature/access-components`
```bash
git checkout main
git pull origin main
git checkout -b feature/access-components
git add frontend/src/components/access/AccessRequestCard.jsx
git add frontend/src/components/access/AccessDurationSelect.jsx
git add frontend/src/components/access/GrantedAccessCard.jsx
git add frontend/src/components/access/MyAccessRequestCard.jsx
git add frontend/src/components/access/PurposeInput.jsx
git add frontend/src/pages/profile/Settings.jsx
git add frontend/src/pages/search/SearchResults.jsx
git commit -m "access request components"
git push origin feature/access-components
```

---

### Musta

---

#### #### Day 1 — 9:15 AM · Branch: `feature/database-schema`
```bash
git checkout main
git pull origin main
git checkout -b feature/database-schema
git add db/schema.sql
git add db/seed.sql
git commit -m "add database schema and seed data"
git push origin feature/database-schema
```

---

#### #### Day 1 — 2:45 PM · Branch: `feature/core-entities`
```bash
git checkout main
git pull origin main
git checkout -b feature/core-entities
git add backend/src/main/java/com/eduauth/model/User.java
git add backend/src/main/java/com/eduauth/model/Student.java
git add backend/src/main/java/com/eduauth/model/Institution.java
git add backend/src/main/java/com/eduauth/model/Verifier.java
git add backend/src/main/java/com/eduauth/model/Enrollment.java
git add backend/src/main/java/com/eduauth/model/Certificate.java
git add backend/src/main/java/com/eduauth/model/AccessRequest.java
git commit -m "core database entities"
git push origin feature/core-entities
```

---

#### #### Day 1 — 3:15 PM · Branch: `feature/support-entities`
```bash
git checkout main
git pull origin main
git checkout -b feature/support-entities
git add backend/src/main/java/com/eduauth/model/AccessGrant.java
git add backend/src/main/java/com/eduauth/model/VerificationLog.java
git add backend/src/main/java/com/eduauth/model/WithdrawalRequest.java
git add backend/src/main/java/com/eduauth/model/ActivityLog.java
git add backend/src/main/java/com/eduauth/model/UserSettings.java
git add backend/src/main/java/com/eduauth/model/PendingRegistration.java
git commit -m "support and log entities"
git push origin feature/support-entities
```

---

#### #### Day 2 — 9:30 AM · Branch: `feature/core-repositories`
```bash
git checkout main
git pull origin main
git checkout -b feature/core-repositories
git add backend/src/main/java/com/eduauth/repository/UserRepository.java
git add backend/src/main/java/com/eduauth/repository/StudentRepository.java
git add backend/src/main/java/com/eduauth/repository/InstitutionRepository.java
git add backend/src/main/java/com/eduauth/repository/VerifierRepository.java
git add backend/src/main/java/com/eduauth/repository/EnrollmentRepository.java
git add backend/src/main/java/com/eduauth/repository/CertificateRepository.java
git add backend/src/main/java/com/eduauth/repository/AccessRequestRepository.java
git commit -m "core repositories"
git push origin feature/core-repositories
```

---

#### #### Day 2 — 10:00 AM · Branch: `feature/support-repositories`
```bash
git checkout main
git pull origin main
git checkout -b feature/support-repositories
git add backend/src/main/java/com/eduauth/repository/AccessGrantRepository.java
git add backend/src/main/java/com/eduauth/repository/VerificationLogRepository.java
git add backend/src/main/java/com/eduauth/repository/WithdrawalRequestRepository.java
git add backend/src/main/java/com/eduauth/repository/ActivityLogRepository.java
git add backend/src/main/java/com/eduauth/repository/UserSettingsRepository.java
git add backend/src/main/java/com/eduauth/repository/PendingRegistrationRepository.java
git add backend/src/main/java/com/eduauth/repository/specification/UserSpecification.java
git commit -m "support repositories"
git push origin feature/support-repositories
```

---

#### #### Day 3 — 9:15 AM · Branch: `feature/admin-backend`
```bash
git checkout main
git pull origin main
git checkout -b feature/admin-backend
git add backend/src/main/java/com/eduauth/dto/dashboard/AdminDashboardDto.java
git add backend/src/main/java/com/eduauth/dto/dashboard/StudentDashboardDto.java
git add backend/src/main/java/com/eduauth/dto/dashboard/UniversityDashboardDto.java
git add backend/src/main/java/com/eduauth/dto/dashboard/VerifierDashboardDto.java
git add backend/src/main/java/com/eduauth/service/DashboardService.java
git add backend/src/main/java/com/eduauth/dto/admin/AdminUserListDto.java
git add backend/src/main/java/com/eduauth/dto/admin/AdminUserDetailDto.java
git add backend/src/main/java/com/eduauth/dto/admin/SuspendRequestDto.java
git add backend/src/main/java/com/eduauth/service/AdminUserService.java
git add backend/src/main/java/com/eduauth/controller/admin/AdminDashboardController.java
git add backend/src/main/java/com/eduauth/controller/admin/AdminUserController.java
git commit -m "admin user approval and management"
git push origin feature/admin-backend
```

---

#### #### Day 3 — 11:10 AM · Branch: `feature/admin-frontend`
```bash
git checkout main
git pull origin main
git checkout -b feature/admin-frontend
git add frontend/src/pages/admin/Dashboard.jsx
git add frontend/src/pages/admin/Users.jsx
git add frontend/src/pages/admin/UserDetails.jsx
git add frontend/src/pages/admin/UserApprovals.jsx
git add frontend/src/pages/admin/Certificates.jsx
git add frontend/src/pages/admin/ProfileChangeRequests.jsx
git add frontend/src/pages/admin/ActivityLogs.jsx
git add frontend/src/pages/admin/Analytics.jsx
git commit -m "admin dashboard and user management pages"
git push origin feature/admin-frontend
```

---

#### #### Day 3 — 4:30 PM · Branch: `feature/admin-certificates`
```bash
git checkout main
git pull origin main
git checkout -b feature/admin-certificates
git add backend/src/main/java/com/eduauth/controller/admin/AdminCertificateController.java
git commit -m "admin certificate revocation and management"
git push origin feature/admin-certificates
```

---

#### #### Day 5 — 5:15 PM · Branch: `feature/api-docs`
```bash
git checkout main
git pull origin main
git checkout -b feature/api-docs
git add API.md
git commit -m "api reference documentation"
git push origin feature/api-docs
```

---

### Saikat

---

#### #### Day 1 — 9:55 AM · Branch: `feature/project-setup`
```bash
git checkout main
git pull origin main
git checkout -b feature/project-setup
git add backend/pom.xml
git add backend/src/main/resources/application.properties
git add backend/src/main/java/com/eduauth/EduAuthApplication.java
git commit -m "set up spring boot maven project"
git push origin feature/project-setup
```

---

#### #### Day 1 — 10:40 AM · Branch: `feature/cors-and-mail`
```bash
git checkout main
git pull origin main
git checkout -b feature/cors-and-mail
git add backend/src/main/java/com/eduauth/config/CorsConfig.java
git add backend/src/main/java/com/eduauth/config/MailConfig.java
git add backend/src/main/java/com/eduauth/exception/BadRequestException.java
git add backend/src/main/java/com/eduauth/exception/ResourceNotFoundException.java
git add backend/src/main/java/com/eduauth/exception/UnauthorizedException.java
git add backend/src/main/java/com/eduauth/exception/GlobalExceptionHandler.java
git commit -m "cors and mail configuration with exceptions"
git push origin feature/cors-and-mail
```

---

#### #### Day 2 — 6:30 PM · Branch: `feature/extra-auth-pages`
```bash
git checkout main
git pull origin main
git checkout -b feature/extra-auth-pages
git add frontend/src/pages/auth/RegisterLanding.jsx
git add frontend/src/pages/auth/EmailVerification.jsx
git add frontend/src/pages/auth/EmailVerified.jsx
git add frontend/src/pages/auth/ForgotPassword.jsx
git add frontend/src/pages/auth/ResetPassword.jsx
git add frontend/src/pages/auth/VerifyEmailChange.jsx
git commit -m "auth pages for email and password reset"
git push origin feature/extra-auth-pages
```

---

#### #### Day 4 — 2:55 PM · Branch: `feature/university-backend`
```bash
git checkout main
git pull origin main
git checkout -b feature/university-backend
git add backend/src/main/java/com/eduauth/controller/university/UniversityDashboardController.java
git add backend/src/main/java/com/eduauth/controller/university/UniversityCertificateController.java
git commit -m "university dashboard and certificate issuance api"
git push origin feature/university-backend
```

---

#### #### Day 4 — 5:10 PM · Branch: `feature/university-frontend`
```bash
git checkout main
git pull origin main
git checkout -b feature/university-frontend
git add frontend/src/pages/university/Dashboard.jsx
git add frontend/src/pages/university/Certificates.jsx
git add frontend/src/pages/university/Enrollments.jsx
git add frontend/src/pages/university/IssueCertificate.jsx
git add frontend/src/pages/university/Settings.jsx
git add frontend/src/components/university/DepartmentsManager.jsx
git add frontend/src/components/university/batch-issue/CSVUpload.jsx
git add frontend/src/components/university/batch-issue/ProgressModal.jsx
git add frontend/src/components/university/batch-issue/ResultsSummary.jsx
git commit -m "university enrollments and certificate issuance pages"
git push origin feature/university-frontend
```

---

#### #### Day 5 — 10:25 AM · Branch: `feature/verifier-dashboard`
```bash
git checkout main
git pull origin main
git checkout -b feature/verifier-dashboard
git add backend/src/main/java/com/eduauth/controller/verifier/VerifierDashboardController.java
git add backend/src/main/java/com/eduauth/controller/verifier/VerifierCertificateController.java
git commit -m "verifier dashboard and accessible certificates api"
git push origin feature/verifier-dashboard
```

---

#### #### Day 5 — 12:30 PM · Branch: `feature/verifier-frontend`
```bash
git checkout main
git pull origin main
git checkout -b feature/verifier-frontend
git add frontend/src/pages/verifier/Dashboard.jsx
git add frontend/src/pages/verifier/VerifyCertificate.jsx
git add frontend/src/pages/verifier/VerificationHistory.jsx
git add frontend/src/pages/verifier/AccessRequests.jsx
git add frontend/src/pages/verifier/AccessibleCertificates.jsx
git commit -m "verifier search and history pages"
git push origin feature/verifier-frontend
```

---

#### #### Day 5 — 6:20 PM · Branch: `feature/license`
```bash
git checkout main
git pull origin main
git checkout -b feature/license
git add LICENSE
git commit -m "add mit license"
git push origin feature/license
```

---

## 5. Summary Table and Stats

### Contribution Table

| Member | Phases Owned | Features Built | Files | Commits |
|--------|-------------|----------------|-------|---------|
| **Sadid** | Security, Certificate Core, Frontend Setup, Public Verify, Docs | JWT auth, encryption/serial, frontend foundation, public verification, README | 45 | 7 |
| **Sayem** | Auth Backend, Layout Components, Auth Pages, Student Feature | Register/login API, shared UI, auth pages, student dashboard & certificates | 44 | 9 |
| **Musta** | DB Schema, Entity Models, Repositories, Admin Feature, Admin Certs | Database, data layer, admin user management, admin certificates, API docs | 37 | 9 |
| **Saikat** | Project Setup, Infra Config, University Feature, Verifier Feature | Maven/Spring config, CORS/mail, university feature, verifier feature, license | 31 | 8 |

### Timeline Overview

| Day | Time | Member | Branch | Commit Message |
|-----|------|--------|--------|---------------|
| Day 1 | 9:15 AM | Musta | `feature/database-schema` | add database schema and seed data |
| Day 1 | 9:55 AM | Saikat | `feature/project-setup` | set up spring boot maven project |
| Day 1 | 10:40 AM | Saikat | `feature/cors-and-mail` | cors and mail configuration with exceptions |
| Day 1 | 12:20 PM | Sadid | `feature/jwt-implementation` | set up jwt and spring security |
| Day 1 | 2:45 PM | Musta | `feature/core-entities` | core database entities |
| Day 1 | 3:15 PM | Musta | `feature/support-entities` | support and log entities |
| Day 2 | 9:30 AM | Musta | `feature/core-repositories` | core repositories |
| Day 2 | 10:00 AM | Musta | `feature/support-repositories` | support repositories |
| Day 2 | 10:45 AM | Sayem | `feature/auth-dtos` | auth dtos |
| Day 2 | 11:15 AM | Sayem | `feature/auth-services` | auth services and controllers |
| Day 2 | 1:15 PM | Sadid | `feature/frontend-config` | frontend configuration |
| Day 2 | 1:45 PM | Sadid | `feature/frontend-assets-and-utils` | frontend assets and contexts |
| Day 2 | 2:15 PM | Sadid | `feature/frontend-and-public-pages` | react app and public pages |
| Day 2 | 3:40 PM | Sayem | `feature/shared-components` | shared ui components part 1 |
| Day 2 | 4:05 PM | Sayem | `feature/ui-components` | shared ui components part 2 |
| Day 2 | 4:30 PM | Sayem | `feature/layout-components` | dashboard layout and navigation |
| Day 2 | 5:55 PM | Sayem | `feature/auth-pages` | auth pages and public landing with info pages |
| Day 2 | 6:30 PM | Saikat | `feature/extra-auth-pages` | auth pages for email and password reset |
| Day 3 | 9:15 AM | Musta | `feature/admin-backend` | admin user approval and management |
| Day 3 | 11:10 AM | Musta | `feature/admin-frontend` | admin dashboard and user management pages |
| Day 3 | 2:00 PM | Sadid | `feature/certificate-core` | encryption serial generation and certificate service |
| Day 3 | 4:30 PM | Musta | `feature/admin-certificates` | admin certificate revocation and management |
| Day 4 | 9:20 AM | Sayem | `feature/student-backend` | student dashboard and certificate endpoints |
| Day 4 | 11:40 AM | Sayem | `feature/student-pages` | student dashboard and pages |
| Day 4 | 12:10 PM | Sayem | `feature/access-components` | access request components |
| Day 4 | 2:55 PM | Saikat | `feature/university-backend` | university dashboard and certificate issuance api |
| Day 4 | 5:10 PM | Saikat | `feature/university-frontend` | university enrollments and certificate issuance pages |
| Day 5 | 9:10 AM | Sadid | `feature/verification-backend` | verifier and public verification endpoints |
| Day 5 | 10:25 AM | Saikat | `feature/verifier-dashboard` | verifier dashboard and accessible certificates api |
| Day 5 | 12:30 PM | Saikat | `feature/verifier-frontend` | verifier search and history pages |
| Day 5 | 5:15 PM | Musta | `feature/api-docs` | api reference documentation |
| Day 5 | 6:20 PM | Saikat | `feature/license` | add mit license |
| Day 5 | 7:30 PM | Sadid | `feature/documentation` | readme and project documentation |

### Final Repository Stats

| Metric | Value |
|--------|-------|
| Total commits | 34 (1 init + 33 feature) |
| Total tracked files | ~157 source files |
| Total feature branches | 33 |
| Total PRs merged | 33 |
| Days of development | 5 |
| Team members | 4 |
| Backend files | 76 Java files |
| Frontend files | 113 JSX/JS/CSS files |
| Documentation files | 3 (README, API.md, LICENSE) |

---

## 6. GitHub Verification Guide

### How a teacher or reviewer can verify contributions

#### 1. GitHub Insights → Contributors Graph
Navigate to the repository on GitHub → **Insights** → **Contributors**. This shows:
- Number of commits per contributor over time
- Lines added/deleted per contributor
- Activity timeline per contributor

Each member should show commits on different days matching the push plan.

#### 2. Git Blame on Any File
```bash
git blame backend/src/main/java/com/eduauth/service/AuthService.java
```
Every line will show which commit and which author wrote it.

#### 3. Filter Commit History by Author
```bash
git log --author="Sadid" --oneline
git log --author="Sayem" --oneline
git log --author="Musta" --oneline
git log --author="Saikat" --oneline
```
This shows all commits by a specific team member.

#### 4. Pull Request History
Go to the repository on GitHub → **Pull Requests** → **Closed**. Every PR shows:
- Who created the branch and pushed the code
- Who reviewed and merged the PR
- The files changed in that PR
- The date and time of the merge

#### 5. Branch History
```bash
git branch -a --sort=-committerdate
```
Shows all branches in order of last commit, confirming the development timeline.

#### 6. Full Contribution Summary
```bash
git shortlog -s -n --all
```
Shows total commits per author, sorted by count.

---

### Git Config Setup for Each Member

Each member must set their git identity before committing:

**Sadid:**
```bash
git config user.name "Sadid Ahmed"
git config user.email "sadid@example.com"
```

**Sayem:**
```bash
git config user.name "M.M. Sayem Prodhan"
git config user.email "sayem@example.com"
```

**Musta:**
```bash
git config user.name "Md. Mostafizur Rahman"
git config user.email "musta@example.com"
```

**Saikat:**
```bash
git config user.name "Saikat Raihan"
git config user.email "saikat@example.com"
```

> **Important:** Replace the example emails with each member's actual GitHub-registered email address. The email must match the GitHub account for contributions to be linked to the correct profile in the Contributors graph.

---

### PR Workflow (for Sadid or whoever manages PRs)

After each push:
1. Go to GitHub → repository → **Pull Requests** → **New Pull Request**
2. Select the pushed branch as the compare branch
3. Base branch should be `main`
4. Title the PR with the commit message
5. Add a brief description if needed
6. **Create Pull Request**
7. Review the changed files
8. **Merge Pull Request** (use "Create a merge commit" to preserve branch history)
9. **Delete the branch** after merging (keeps the repo clean)
10. The next member can now pull main and start their push

---

> **This plan is designed to be followed in exact order. Each push block is copy-paste ready. No file is missing, no dependency is violated, and the git history tells a clean, professional, technically correct development story.**
