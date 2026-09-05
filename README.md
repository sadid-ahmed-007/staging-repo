# EduAuth Registry

**A secure platform for issuing, managing, and verifying academic certificates.**

![Java 17](https://img.shields.io/badge/Java-17-007396?logo=java) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-6DB33F?logo=springboot) ![React](https://img.shields.io/badge/React-18-61DAFB?logo=react) ![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql) ![License](https://img.shields.io/badge/License-MIT-green)

---

EduAuth Registry is a full-stack web application that enables universities to issue tamper-evident digital academic certificates, students to manage and share their credentials, and employers or institutions to verify authenticity in real time. The system supports role-based access across four user types with JWT-based authentication and OTP email verification.

## Features

- Role-based access control (Student, University, Verifier, Admin)
- JWT authentication with OTP email verification
- Certificate issuance with unique serial numbers and checksum validation
- PDF certificate generation with embedded QR codes (iText 7 + ZXing)
- Public/private visibility toggle per certificate
- Share-link verification with encrypted date-of-birth tokens
- Verification logging with full audit history and CSV export
- Admin controls for user approval, suspension, and certificate revocation

## Tech Stack

| Layer        | Technology                           |
|--------------|--------------------------------------|
| Backend      | Java 17, Spring Boot 3.3.2, Spring Security |
| Frontend     | React 18, Vite, Tailwind CSS         |
| Database     | MySQL 8                              |
| Security     | JWT (jjwt 0.12.6), BCrypt            |
| Build Tool   | Maven 3.9.6 (bundled), npm           |
| PDF          | iText 7                              |
| QR Code      | ZXing 3.5.3 (backend), qrcode.react (frontend) |
| Email        | Spring Mail (Gmail SMTP)             |

## User Roles

| Role        | Capabilities                                                   |
|-------------|----------------------------------------------------------------|
| Student     | View and download own certificates, toggle visibility, share links |
| University  | Issue certificates to students, view issued certificates       |
| Verifier    | Verify certificates by serial, view and export verification history |
| Admin       | Manage all users and certificates, revoke/restore, approve accounts |

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+ (or use the bundled `backend/apache-maven-3.9.6`)
- MySQL 8+
- Node.js 18+

### Database Setup

```sql
CREATE DATABASE eduauth_registry;
```

Then run the schema and seed files:

```bash
mysql -u root -p eduauth_registry < db/schema.sql
mysql -u root -p eduauth_registry < db/seed.sql
```

### Backend Setup

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/eduauth_registry?useSSL=false&serverTimezone=Asia/Dhaka
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

jwt.secret=YOUR_256_BIT_BASE64_SECRET

spring.mail.username=YOUR_GMAIL
spring.mail.password=YOUR_APP_PASSWORD
```

```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
```

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:5173
```

### Test Accounts

| Role        | Email                    | Password     |
|-------------|--------------------------|--------------|
| Admin       | eduauthregistry@gmail.com      | admin123     |
| Student     | ssadidahmed01@gmail.com        | password123  |
| University  | admin@uiu.ac.bd           | password123  |
| Verifier    | demo@enosis.com     | password123  |

## Project Structure

```
eduauth-registry-maven/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/eduauth/
│       ├── controller/       # REST controllers (auth, student, university, verifier, admin, public)
│       ├── model/            # JPA entities
│       ├── repository/       # Spring Data JPA repositories
│       ├── service/          # Business logic
│       ├── dto/              # Request / response DTOs
│       └── config/           # Security, CORS, JWT configuration
├── frontend/
│   ├── src/
│   │   ├── pages/            # Route-level page components
│   │   ├── components/       # Shared UI components
│   │   └── api/              # Axios API client
│   └── package.json
├── db/
│   ├── schema.sql
│   └── seed.sql
├── API.md
└── LICENSE
```

## API Reference

See [API.md](API.md) for the complete API reference.

## License

Licensed under the MIT License. See [LICENSE](LICENSE).

## Team

**Team PhaseShift** — United International University
Advanced Object-Oriented Programming

| Member                  |
|-------------------------|
| Sadid Ahmed             |
| M.M. Sayem Prodhan      |
| Saikat Raihan           |
| Md. Mostafizur Rahman   |
