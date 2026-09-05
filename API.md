# EduAuth Registry API Reference

**Base URL:** `http://localhost:8080/api`
**Auth:** Bearer token in `Authorization` header
**Last Updated:** 2026-08-31

### Standard Response Structure
```json
// Success
{ "success": true, "message": "...", "data": {} }
// Error
{ "success": false, "message": "...", "errors": {} }
```

### Public Endpoints
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| GET | `/verify/system-stats` | No | Get system statistics |
| POST | `/verify/certificate` | No | Verify certificate manually |
| GET | `/verify/link` | No | Verify certificate from link |

**Request Details:**
- `POST /verify/certificate`: Body `{ serial: String, date_of_birth: String }`
- `GET /verify/link`: Query `s` (serial), `v` (dobToken)

### Auth Endpoints
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| POST | `/auth/register` | No | Register new user account |
| POST | `/auth/verify-email` | No | Verify user email address |
| POST | `/auth/resend-verification` | No | Resend verification code |
| POST | `/auth/login` | No | Authenticate and get token |
| POST | `/auth/logout` | Yes | Invalidate user session |
| GET | `/auth/me` | Yes | Get current profile details |

**Request Details:**
- `POST /auth/register`: Body `{ email: String, password: String, confirmPassword: String, role: String, firstName: String, lastName: String, dateOfBirth: String, gender: String, nid: String, phone: String, address: String, website: String, institutionName: String, registrationNumber: String, city: String, companyName: String, contactPerson: String, designation: String, purpose: String }`
- `POST /auth/verify-email`: Body `{ email: String, code: String }`
- `POST /auth/resend-verification`: Body `{ email: String }`
- `POST /auth/login`: Body `{ email: String, password: String }`

### Student Endpoints
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| GET | `/student/dashboard` | Yes | Get student dashboard stats |
| GET | `/student/certificates` | Yes | List student certificates |
| GET | `/student/certificates/{id}` | Yes | Get specific certificate details |
| GET | `/student/certificates/{id}/pdf` | Yes | Download certificate PDF |
| PATCH | `/student/certificates/{id}/visibility` | Yes | Toggle certificate visibility |

**Request Details:**
- `GET /student/certificates`: Query `filter`, `page`, `size`
- `PATCH /student/certificates/{id}/visibility`: Body `{ isPubliclyShareable: boolean }`

### University Endpoints
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| GET | `/university/dashboard` | Yes | Get university dashboard stats |
| GET | `/university/certificates` | Yes | List issued certificates |
| GET | `/university/certificates/{id}` | Yes | Get issued certificate details |

### Verifier Endpoints
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| GET | `/verifier/dashboard` | Yes | Get verifier dashboard stats |
| GET | `/verifier/verifications/stats` | Yes | Get verification statistics |
| POST | `/verifier/verify` | Yes | Verify certificate securely |
| GET | `/verifier/verifications/recent` | Yes | List recent verifications |
| GET | `/verifier/verifications/history` | Yes | List verification history |
| GET | `/verifier/verifications/export` | Yes | Export verification history |
| GET | `/verifier/accessible-certificates` | Yes | List accessible certificates |
| GET | `/verifier/accessible-certificates/{studentId}`| Yes | Get accessible certificate details |

**Request Details:**
- `POST /verifier/verify`: Body `{ serial: String, date_of_birth: String }`
- `GET /verifier/verifications/history`: Query `page`, `status`, `serial`, `from`, `to`
- `GET /verifier/verifications/export`: Query `status`, `serial`, `from`, `to`

### Admin Endpoints
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| GET | `/admin/dashboard` | Yes | Get admin dashboard stats |
| GET | `/admin/users` | Yes | List all users |
| GET | `/admin/users/{id}` | Yes | Get user details |
| POST | `/admin/users/{id}/suspend` | Yes | Suspend a user account |
| POST | `/admin/users/{id}/unsuspend` | Yes | Unsuspend a user account |
| POST | `/admin/users/{id}/approve` | Yes | Approve pending user accounts |
| GET | `/admin/certificates` | Yes | List all certificates |
| GET | `/admin/certificates/{id}` | Yes | Get certificate details |
| POST | `/admin/certificates/{id}/revoke` | Yes | Revoke a certificate |
| POST | `/admin/certificates/{id}/restore` | Yes | Restore revoked certificate |
