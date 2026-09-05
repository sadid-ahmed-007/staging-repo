# EduAuth Registry — Complete System Documentation

This document explains every moving part of the EduAuth Registry system. It is written so that after reading it, you can answer any question a professor asks during a project review — without looking at the code.

---

## 1. System Architecture Overview

The EduAuth Registry is a three-tier web application:

```
┌──────────────┐       HTTP/JSON        ┌──────────────────┐        JDBC        ┌──────────┐
│   Browser    │ ◄─────────────────────► │   Spring Boot    │ ◄────────────────► │  MySQL   │
│  (React/Vite)│      port 5173          │  REST API Server │     port 3306      │ Database │
│              │         ↕               │   port 8080      │                    │          │
│  localStorage│   Axios + Bearer JWT    │                  │                    │          │
│  stores JWT  │                         │  SecurityFilter  │                    │          │
└──────────────┘                         └──────────────────┘                    └──────────┘
```

**Why are they separated?** The frontend is a single-page React application served by Vite's development server on port 5173. The backend is a Spring Boot REST API on port 8080. They are separate processes with separate responsibilities: the frontend handles user interface rendering and client-side state; the backend handles business logic, database access, and security. This separation means we can develop, test, and deploy them independently. It also means any future mobile app or third-party client could consume the same API.

**How they communicate.** Every interaction between the frontend and backend happens through HTTP requests carrying JSON bodies. The frontend uses the Axios HTTP client library to send requests. The backend exposes RESTful endpoints under the `/api` prefix. Request and response bodies are always JSON — never HTML, never form-encoded.

**What is JWT and why we use it.** JWT (JSON Web Token) is a compact, self-contained token that the server generates after a successful login. It contains three base64-encoded parts separated by dots: a header (algorithm metadata), a payload (user data like userId, email, role), and a signature (cryptographic proof that the payload has not been tampered with). We use JWT instead of server-side sessions because our architecture is *stateless*. The Spring Boot server never stores session data in memory or in a database. Every request carries the complete proof of identity inside the `Authorization: Bearer <token>` header. This makes the system horizontally scalable — you could run ten backend instances behind a load balancer and none of them need to share session state.

**Where the token is stored.** The JWT is stored in the browser's `localStorage` under the key `token`. We chose `localStorage` over cookies because our architecture uses explicit `Authorization` headers, not automatic cookie-based authentication. This eliminates the need for CSRF protection entirely (since cookies are never sent automatically) and gives us full control over when and how the token is attached to requests.

**What happens when the browser loads the app.** When a user opens `http://localhost:5173`, the Vite dev server serves the React SPA. React mounts the `App` component (`frontend/src/App.jsx`). The `App` is wrapped in an `AuthProvider` (`frontend/src/contexts/AuthContext.jsx`, line 7). Inside `AuthProvider`, the `useEffect` on line 21 fires immediately. It calls `authService.getStoredUser()` (`frontend/src/services/authService.js`, line 49), which reads `localStorage.getItem('user')` and parses it. If a stored user object exists, the user state is populated and the app shows the authenticated UI. If not, the user sees the public landing page. The `loading` flag is set to `false` on line 28, which triggers a re-render and reveals the actual page content instead of the loading spinner.

---

## 2. Click to Response: User Login

This section traces the complete login flow from a single button click to the user seeing their dashboard.

### STEP 1 — User clicks the Login button

**File:** `frontend/src/pages/auth/Login.jsx`

The `Login` page component is defined at line 7. Its only real job is layout — it renders a split-panel page with branding on the left and the actual login form on the right. The login form itself is the `LoginForm` component, rendered at line 60:

```jsx
<LoginForm />
```

**File:** `frontend/src/components/auth/LoginForm.jsx`

This is where the action happens. The component uses `react-hook-form` with `yup` validation (lines 3–4). A validation schema is defined at lines 11–14:

```javascript
const loginSchema = yup.object().shape({
  email: yup.string().email('Please enter a valid email address').required('Email is required'),
  password: yup.string().required('Password is required'),
});
```

When the user clicks the "Sign In" button (line 86–88), the `handleSubmit` wrapper from `react-hook-form` runs the `yup` validation first. If validation passes, it calls the `onSubmit` function defined at line 29. The first thing `onSubmit` does is clear any previous server error (line 30). Then at line 33 it calls:

```javascript
const response = await login(data);
```

Here, `login` comes from `useAuth()` on line 18 — it is the `login` function provided by `AuthContext`.

### STEP 2 — The API call leaves the browser

**File:** `frontend/src/contexts/AuthContext.jsx`

The `login` function is defined at line 31:

```javascript
const login = useCallback(async (credentials) => {
  const data = await authService.login(credentials);
  setUser(data.user);
  fetchSettings();
  return data;
}, [fetchSettings]);
```

It delegates to `authService.login()`.

**File:** `frontend/src/services/authService.js`

The `login` method is at lines 9–16:

```javascript
login: async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  if (response.data.token) {
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('user', JSON.stringify(response.data.user));
  }
  return response.data;
},
```

This calls `api.post('/auth/login', credentials)`.

**File:** `frontend/src/services/api.js`

The `api` object is an Axios instance created at lines 6–13:

```javascript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  withCredentials: false,
});
```

The `baseURL` tells Axios where to send every request. A call to `api.post('/auth/login', ...)` translates to `POST http://localhost:8080/api/auth/login`. The `withCredentials: false` setting means cookies are never sent — this is deliberate because we use JWT Bearer tokens.

Before every request, the request interceptor at lines 15–26 runs:

```javascript
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  ...
);
```

It reads the JWT from `localStorage` and attaches it as `Authorization: Bearer <token>`. For the login request specifically, there is no token yet (the user is not logged in), so this header is absent. That is fine because the login endpoint is public.

### STEP 3 — Request arrives at Spring Boot

**File:** `backend/src/main/java/com/eduauth/config/JwtAuthenticationFilter.java`

Every HTTP request passes through `JwtAuthenticationFilter` before reaching any controller. This filter extends `OncePerRequestFilter` (line 29), guaranteeing it runs exactly once per request.

The `doFilterInternal` method starts at line 44. At line 50, it reads the `Authorization` header:

```java
String authHeader = request.getHeader("Authorization");
```

For the login request, there is no `Authorization` header at all, so `authHeader` is `null`. The check at lines 53–56 catches this:

```java
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```

The filter immediately passes the request along the filter chain without setting any authentication. The request continues to Spring Security's authorization rules.

**File:** `backend/src/main/java/com/eduauth/config/SecurityConfig.java`

Spring Security decides whether an unauthenticated request is allowed by checking the route rules in `filterChain()` at lines 87–121. The login endpoint is explicitly listed as public at lines 90–96:

```java
.requestMatchers(
        org.springframework.http.HttpMethod.POST,
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/verify-email",
        "/api/auth/resend-verification"
).permitAll()
```

Because `POST /api/auth/login` is `permitAll()`, Spring Security allows the request through even without authentication. The request reaches the controller.

### STEP 4 — Spring Security routes to the controller

**File:** `backend/src/main/java/com/eduauth/controller/AuthController.java`

The class is annotated with `@RequestMapping("/api/auth")` at line 15, and the login method is at lines 46–51:

```java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ipAddress = httpRequest.getRemoteAddr();
    AuthResponse response = authService.login(request, ipAddress);
    return ResponseEntity.ok(response);
}
```

The `@PostMapping("/login")` on line 46 maps `POST /api/auth/login` to this method. The `@Valid @RequestBody LoginRequest request` parameter tells Spring to deserialize the JSON body `{"email":"...", "password":"..."}` into a `LoginRequest` DTO and validate it. The `HttpServletRequest httpRequest` parameter is injected by Spring to give access to the raw HTTP request — here we use it to extract the client's IP address for activity logging.

The method calls `authService.login(request, ipAddress)` at line 49.

### STEP 5 — AuthService processes the login

**File:** `backend/src/main/java/com/eduauth/service/AuthService.java`

The `login()` method starts at line 178:

```java
@Transactional
public AuthResponse login(LoginRequest request, String ipAddress) {
```

It performs five sequential checks:

**Check 1 — Find user by email (line 180):**

```java
User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
```

The `userRepository.findByEmail()` queries the `users` table using `WHERE email = ?`. It returns an `Optional<User>`. If no user exists with that email, `orElseThrow` throws an `UnauthorizedException` with the message "Invalid email or password". The message is deliberately vague — we never reveal whether the email exists or the password is wrong, to prevent account enumeration attacks.

**Check 2 — Verify password with BCrypt (line 183):**

```java
if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
    throw new UnauthorizedException("Invalid email or password");
}
```

`passwordEncoder` is a `BCryptPasswordEncoder` bean defined in `SecurityConfig.java` at lines 52–55. The `matches()` method takes the raw password the user typed and the BCrypt hash stored in the database. BCrypt internally extracts the salt and cost factor from the stored hash, re-hashes the raw password with the same parameters, and compares the results. If they do not match, login fails.

**Check 3 — Email verification (line 187):**

```java
if (user.getEmailVerifiedAt() == null) {
    throw new UnauthorizedException("Please verify your email first");
}
```

The `email_verified_at` column is `null` until the user completes OTP verification. If it is still `null`, the user cannot log in.

**Check 4 — Admin approval (line 191):**

```java
if (!user.getIsApproved()) {
    throw new UnauthorizedException("Your account is pending admin approval");
}
```

Every new account starts with `is_approved = false`. An admin must manually approve the account before the user can log in.

**Check 5 — Suspension check (line 195):**

```java
if (user.getSuspendedAt() != null) {
    throw new UnauthorizedException("Your account has been suspended");
}
```

If the `suspended_at` timestamp is not null, the account has been suspended by an admin.

If all five checks pass, the method proceeds to generate a JWT token at line 199:

```java
String token = jwtService.generateToken(user);
```

It then logs the login activity at line 201 and builds the response (lines 203–227), which includes the token and the user profile data (fetched from the appropriate role-specific repository — `studentRepository`, `institutionRepository`, or `verifierRepository`).

### STEP 6 — JWT token is generated

**File:** `backend/src/main/java/com/eduauth/service/JwtService.java`

The `generateToken()` method starts at line 43:

```java
public String generateToken(User user) {
    Date now    = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim("email", user.getEmail())
            .claim("role",  user.getRole())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
}
```

A **claim** is a key-value pair stored in the JWT payload. This token contains these claims:

- `sub` (subject) — the user's database ID, converted to a string (line 48). This is the standard JWT subject claim.
- `email` — the user's email address (line 49).
- `role` — the user's role string: `"student"`, `"university"`, `"verifier"`, or `"admin"` (line 50).
- `iat` (issued at) — the current timestamp (line 51).
- `exp` (expiration) — `now + expirationMs` (line 52). The `expirationMs` value comes from `application.properties` (line 19): `jwt.expiration-ms=604800000`, which is 7 days in milliseconds.

The token is **signed** using HMAC-SHA256 (`Jwts.SIG.HS256`) at line 53. The secret key is derived at startup in the `initKey()` method (lines 27–31):

```java
@PostConstruct
private void initKey() {
    this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
}
```

The `secretString` comes from `application.properties` (line 18): `jwt.secret=your-256-bit-base64-secret-key-here-replace-this`. The `Keys.hmacShaKeyFor()` method converts this string to a `SecretKey` suitable for HMAC-SHA256.

The `compact()` call at line 54 serializes the token into its final three-part string:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1IiwiZW1haWwiOiJzdHVkZW50QHRlc3QuY29tIiwicm9sZSI6InN0dWRlbnQiLCJpYXQiOjE3MjUxMDAwMDAsImV4cCI6MTcyNTcwNDgwMH0.HMAC_SIGNATURE_HERE
```

The three parts are: `header.payload.signature`, each base64url-encoded.

### STEP 7 — Response travels back to frontend

The `AuthService.login()` method returns an `AuthResponse` object built at lines 216–226:

```java
return AuthResponse.builder()
        .token(token)
        .user(AuthResponse.UserPayload.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isApproved(user.getIsApproved())
                .emailVerified(user.getEmailVerifiedAt() != null)
                .profile(profile)
                .build())
        .build();
```

Back in `AuthController` (line 50), this is returned as `ResponseEntity.ok(response)`, which Spring serializes to JSON:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 5,
    "email": "student@test.com",
    "role": "student",
    "isApproved": true,
    "emailVerified": true,
    "profile": {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "dateOfBirth": "2000-01-15"
    }
  }
}
```

### STEP 8 — Frontend receives the response

**File:** `frontend/src/services/authService.js`

The `login` method at lines 9–16 receives this response. Lines 11–13 store the token and user:

```javascript
if (response.data.token) {
  localStorage.setItem('token', response.data.token);
  localStorage.setItem('user', JSON.stringify(response.data.user));
}
```

The token is stored under the key `'token'` in `localStorage`. The user object is JSON-stringified and stored under `'user'`. This is what the request interceptor in `api.js` reads on subsequent requests.

**File:** `frontend/src/contexts/AuthContext.jsx`

Back in `AuthContext.login()` at line 33, the user state is updated:

```javascript
const data = await authService.login(credentials);
setUser(data.user);
fetchSettings();
return data;
```

`setUser(data.user)` triggers a React re-render. Every component that uses `useAuth()` now sees the updated user. The `fetchSettings()` call loads user preferences (like theme) from the server.

### STEP 9 — Router redirects to dashboard

**File:** `frontend/src/components/auth/LoginForm.jsx`

Back in the `onSubmit` function (lines 34–38), after the `login()` call returns:

```javascript
toast.success('Logged in successfully');
if (response.user.role === 'student') navigate('/student/dashboard');
else if (response.user.role === 'university') navigate('/university/dashboard');
else if (response.user.role === 'verifier') navigate('/verifier/dashboard');
else if (response.user.role === 'admin') navigate('/admin/dashboard');
```

The `navigate` function (from `react-router-dom`) programmatically changes the URL to the appropriate dashboard based on the user's role.

**File:** `frontend/src/App.jsx`

The route `/student/dashboard` is defined inside a `ProtectedRoute` wrapper at lines 130–136:

```jsx
<Route element={<ProtectedRoute allowedRoles={['student']} />}>
  <Route path="/student/dashboard" element={<StudentDashboard />} />
  ...
</Route>
```

**File:** `frontend/src/components/auth/ProtectedRoute.jsx`

The `ProtectedRoute` component (lines 5–25) enforces three checks:

```javascript
export default function ProtectedRoute({ allowedRoles = [] }) {
  const { user, loading } = useAuth();

  if (loading) {
    return ( <div>...</div> ); // Show spinner
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
```

First, if `loading` is true (line 8), it shows a spinner — this prevents a flash of the login page while `AuthContext` is still reading from `localStorage`. Second, if `user` is null (line 16), the user is redirected to `/login`. Third, if `allowedRoles` is specified and the user's role is not in the list (line 20), the user is redirected to the homepage. If all checks pass, `<Outlet />` renders the nested child route — in this case, `<StudentDashboard />`.

---

## 3. Click to Response: Certificate Verification

This section traces what happens when a user opens a share link like:

```
http://localhost:5173/verify?s=BSC-26-000001M&v=c29tZUVuY3J5cHRlZFRva2Vu...
```

### STEP 1 — Browser opens the share link

**File:** `frontend/src/pages/public/VerifyCertificate.jsx`

React Router matches `/verify` to the `VerifyCertificate` component (defined in `App.jsx` line 109):

```jsx
<Route path="/verify" element={<VerifyCertificate />} />
```

Inside `VerifyCertificate`, the URL query parameters are read using `useSearchParams` at line 21:

```javascript
const [searchParams] = useSearchParams();
```

The `useEffect` hook at lines 33–64 fires on component mount. It reads the `s` and `v` parameters at lines 37–38:

```javascript
const serial = searchParams.get('s');
const token = searchParams.get('v');
```

When both `serial` and `token` are present (line 40), this is an encrypted share link. The component sets a ref flag to prevent double-execution in React StrictMode (line 42) and calls `autoVerifyFromLink(serial, token)` at line 43.

### STEP 2 — Auto-verification API call

The `autoVerifyFromLink` function is defined at lines 66–95:

```javascript
const autoVerifyFromLink = async (serial, token) => {
  setAutoVerifying(true);
  setError('');
  setResult(null);

  try {
    const { data } = await api.get('/verify/link', {
      params: { s: serial, v: token },
    });
    setResult(data);
  } catch (requestError) { ... }
};
```

It calls `GET /api/verify/link?s=BSC-26-000001M&v=encryptedToken` (line 72). The params are sent as URL query parameters. This call does **not** need an auth token because the `/api/verify/**` endpoints are `permitAll()` in `SecurityConfig.java` (lines 97–106). The Axios request interceptor still runs and would attach a token if one exists in `localStorage`, but the backend ignores it for public endpoints.

### STEP 3 — Request hits the backend

**File:** `backend/src/main/java/com/eduauth/controller/publics/VerifyController.java`

The class is mapped to `/api/verify` at line 27. The share link endpoint is at lines 74–97:

```java
@GetMapping("/link")
@Transactional
public ResponseEntity<?> verifyFromLink(
        @RequestParam("s") String serial,
        @RequestParam("v") String dobToken,
        HttpServletRequest httpRequest) {
```

Spring extracts the `s` parameter into `serial` and the `v` parameter into `dobToken`.

At line 88, the encrypted DOB token is decrypted:

```java
String dob = encryptionService.decryptDOB(dobToken);
```

If decryption fails (returns `null`), a 400 error is returned at lines 90–94. If decryption succeeds, the method delegates to `doVerify(serial, dob, null, true, httpRequest)` at line 96.

### STEP 4 — DOB is decrypted

**File:** `backend/src/main/java/com/eduauth/service/EncryptionService.java`

The `decryptDOB()` method at lines 71–97 reverses the encryption:

```java
public String decryptDOB(String token) {
    try {
        byte[] combined = Base64.getUrlDecoder().decode(token);
```

Line 73: The URL-safe Base64 string is decoded back to raw bytes. URL-safe Base64 uses `-` and `_` instead of `+` and `/` (which have special meanings in URLs).

```java
        if (combined.length < IV_LENGTH + 1) {
            return null;
        }
```

Lines 74–76: Sanity check — the decoded data must be at least 17 bytes (16 for the IV plus at least 1 byte of ciphertext).

```java
        byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
```

Lines 78–79: The first 16 bytes are the Initialization Vector (IV). The rest is the AES ciphertext. The IV was prepended during encryption.

```java
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(encrypted);
```

Lines 81–83: An AES cipher in CBC mode with PKCS5 padding (`AES/CBC/PKCS5Padding`, defined on line 26) is initialized in `DECRYPT_MODE` with the secret key and the extracted IV. The `doFinal()` call decrypts the ciphertext.

The secret key was derived in the constructor at lines 29–37:

```java
public EncryptionService(@Value("${jwt.secret}") String jwtSecret) {
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] keyBytes = sha256.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
}
```

The JWT secret string is hashed with SHA-256 to produce a 256-bit (32-byte) key suitable for AES-256.

```java
        String dob = new String(decrypted, StandardCharsets.UTF_8);
        LocalDate parsed = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
```

Lines 85–88: The decrypted bytes are converted to a string. It is then parsed as a date to validate that the decrypted output is actually a valid date in `yyyy-MM-dd` format. If parsing fails or the round-trip does not match, `null` is returned. A successful decryption produces something like `"2000-01-15"`.

### STEP 5 — Checksum is validated

**File:** `backend/src/main/java/com/eduauth/service/SerialGeneratorService.java`

Back in `VerifyController.doVerify()` (line 109), the first check is the serial's checksum:

```java
if (!SerialGeneratorService.validateChecksum(serial)) {
```

The `validateChecksum()` method is at lines 31–57:

```java
public static boolean validateChecksum(String serial) {
    if (serial == null) return false;

    String[] parts = serial.split("-");
    if (parts.length != 3) return false;
```

A valid serial looks like `BSC-26-000001M`. Splitting by `-` produces three parts: `["BSC", "26", "000001M"]`.

```java
    String prefix = parts[0];            // "BSC"
    String year = parts[1];              // "26"
    String seqWithChecksum = parts[2];   // "000001M"

    if (seqWithChecksum.length() != 7) return false;

    String sequence = seqWithChecksum.substring(0, 6);           // "000001"
    String providedChecksum = seqWithChecksum.substring(6, 7);   // "M"
```

The third part must be exactly 7 characters: 6 digits of the sequence number plus 1 checksum character.

```java
    String expectedChecksum = calculateChecksum(prefix, year, sequence);
    return providedChecksum.equals(expectedChecksum);
```

The `calculateChecksum()` method at lines 12–26 computes the expected checksum:

```java
private static String calculateChecksum(String prefix, String year, String sequence) {
    String charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    String data = prefix + year + sequence;    // "BSC26000001"
    int sum = 0;
    for (int i = 0; i < data.length(); i++) {
        sum += data.charAt(i);
    }
    int index = sum % charset.length();
    return String.valueOf(charset.charAt(index));
}
```

The algorithm concatenates the prefix, year, and sequence into a single string (e.g., `"BSC26000001"`). It sums the ASCII values of all characters: `B`=66, `S`=83, `C`=67, `2`=50, `6`=54, `0`=48×5, `1`=49. Total sum = 66+83+67+50+54+48+48+48+48+48+49 = 609. Then `609 % 31 = 21` (the charset length is 31, not 32, because four confusing characters — `0`, `O`, `I`, `1` — are excluded). Index 21 maps to `'V'` in the charset. So the checksum character for this serial would be `V`.

If the provided checksum character does not match the calculated one, the serial has been tampered with or typed incorrectly, and verification stops immediately with an `invalid_checksum` response.

### STEP 6 — Certificate is found in database

In `VerifyController.doVerify()` at line 119:

```java
Optional<Certificate> certOpt = certificateRepository.findBySerial(serial);
if (certOpt.isEmpty()) {
    logVerification(null, serial, dateOfBirthStr, false, "not_found", ...);
    return ResponseEntity.status(404).body(Map.of(...));
}
```

The repository method queries `SELECT * FROM certificates WHERE serial = ?`. If no certificate exists with that serial, a 404 response is returned with a `"not_found"` status.

### STEP 7 — Date of birth is compared

At lines 162–171 of `VerifyController.java`:

```java
String studentDob = certificate.getStudent().getDateOfBirth()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
if (!studentDob.equals(dateOfBirthStr)) {
    logVerification(certificate.getId(), serial, dateOfBirthStr, false, "dob_mismatch", ...);
    return ResponseEntity.status(401).body(Map.of(
            "success", false,
            "verified", false,
            "message", "Date of birth does not match our records"));
}
```

The student's date of birth from the database (`certificate.getStudent().getDateOfBirth()`) is formatted as a `yyyy-MM-dd` string and compared directly with the decrypted DOB from the share link. Both are `String` comparisons of the same format, so this is reliable.

Note that a wrong DOB returns "Date of birth does not match our records" rather than "Certificate not found." While the status code differs (401 vs 404), this is a deliberate choice — we confirm the serial exists but the DOB is wrong. However, for share links this scenario essentially cannot happen because the DOB is embedded encrypted inside the link itself.

### STEP 8 — Verification is logged

**File:** `backend/src/main/java/com/eduauth/controller/publics/VerifyController.java`

The `logVerification()` method at lines 234–257 creates a `VerificationLog` entry:

```java
private void logVerification(
        Long certificateId, String serial, String dateOfBirthStr,
        boolean matchedByDob, String result, String details,
        Long verifierId, HttpServletRequest httpRequest) {

    VerificationLog log = VerificationLog.builder()
            .certificateId(certificateId)
            .verifierId(verifierId)
            .serial(serial)
            .enteredDateOfBirth(enteredDob)
            .matchedByDob(matchedByDob)
            .verificationResult(result)
            .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : null)
            .userAgent(httpRequest != null ? httpRequest.getHeader("User-Agent") : null)
            .details(details)
            .build();

    verificationLogRepository.save(log);
}
```

**File:** `backend/src/main/java/com/eduauth/model/VerificationLog.java`

The `VerificationLog` entity maps to the `verification_logs` table. It records the certificate ID, the serial entered, the DOB entered, whether the DOB matched, the outcome string (`"success"`, `"not_found"`, `"dob_mismatch"`, `"revoked"`, `"invalid_checksum"`), the client's IP address, and the browser's User-Agent string.

The `verifierId` is `null` for public verifications (share link or manual). It would be set to the authenticated verifier's user ID when a registered verifier performs verification through their dashboard.

The `@PrePersist` callback at lines 67–74 automatically sets `verifiedAt`, `createdAt`, and `updatedAt` timestamps.

### STEP 9 — Response returned and displayed

For a successful verification, `VerifyController.doVerify()` at lines 185–196 builds the response:

```java
Map<String, Object> certDetails = buildCertificateDetails(certificate);

Map<String, Object> successResponse = new HashMap<>();
successResponse.put("success", true);
successResponse.put("verified", true);
successResponse.put("message", "Certificate verified successfully");
successResponse.put("certificate", certDetails);
return ResponseEntity.ok(successResponse);
```

The `buildCertificateDetails()` method at lines 201–230 assembles a map containing the serial, student name, student ID, certificate level, program, major, CGPA, issue date, institution name, and more.

**File:** `frontend/src/pages/public/VerifyCertificate.jsx`

Back in `autoVerifyFromLink()`, line 75 stores the result:

```javascript
setResult(data);
```

This triggers a re-render. The result modal at line 335 opens (`open={!!result}`). The component checks `result.verified` at line 348:

- If `result.verified` is `true` — the success view is shown (green checkmark icon, certificate details table with serial, student name, institution, degree, CGPA, etc., plus Print and Save PDF buttons).
- If `result.status === 'revoked'` (line 386) — a warning icon is shown with the revocation date and reason.
- Otherwise (line 411) — a red X icon is shown with the error message and troubleshooting tips.

---

## 4. How the Certificate PDF is Generated

### STEP 1 — Button click in frontend

**File:** `frontend/src/pages/student/Certificates.jsx`

The download button is at lines 257–268:

```jsx
<Button
  variant="ghost"
  size="sm"
  onClick={() => handleDownloadPdf(certificate)}
  disabled={downloadingId === certificate.id}
>
  {downloadingId === certificate.id ? (
    <Loader2 className="w-4 h-4 animate-spin" />
  ) : (
    <Download className="w-4 h-4" />
  )}
</Button>
```

The `handleDownloadPdf` function at lines 81–104 calls:

```javascript
await downloadCertificatePDF(certificate.id, certificate.serial, '/student/certificates');
```

**File:** `frontend/src/services/certificateService.js`

The `downloadCertificatePDF` function at lines 12–22:

```javascript
export const downloadCertificatePDF = async (certificateId, serial, basePath = '/certificates') => {
  const url = await createPdfBlobUrl(certificateId, basePath);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `${serial || `certificate-${certificateId}`}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
```

The `createPdfBlobUrl` function at lines 3–10 makes the actual API call:

```javascript
const createPdfBlobUrl = async (certificateId, basePath = '/certificates') => {
  const response = await api.get(`${basePath}/${certificateId}/pdf`, {
    responseType: 'blob',
  });
  const blob = new Blob([response.data], { type: 'application/pdf' });
  return window.URL.createObjectURL(blob);
};
```

The `responseType: 'blob'` is critical. Without it, Axios would try to parse the binary PDF data as JSON text, corrupting it. With `'blob'`, Axios treats the response body as raw binary data and wraps it in a `Blob` object.

The `URL.createObjectURL(blob)` call creates a temporary in-memory URL (like `blob:http://localhost:5173/abc123`) pointing to the PDF data. A hidden `<a>` link element is created, given the `download` attribute (which forces the browser to download instead of navigate), clicked programmatically, and then removed.

### STEP 2 — Request reaches backend

**File:** `backend/src/main/java/com/eduauth/controller/student/StudentCertificateController.java`

The PDF endpoint at lines 167–187:

```java
@GetMapping("/{id}/pdf")
public ResponseEntity<?> downloadPdf(
        @AuthenticationPrincipal User user,
        @PathVariable Long id) {

    Student student = studentRepository.findByUserId(user.getId()).orElse(null);
    if (student == null) {
        return ResponseEntity.status(404)
                .body(Map.of("success", false, "message", "Student profile not found"));
    }

    Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
    if (cert == null || !cert.getStudentId().equals(student.getId())) {
        return ResponseEntity.status(cert == null ? 404 : 403)
                .body(Map.of("success", false, "message", cert == null ? "Certificate not found" : "Access denied"));
    }

    return certificateService.generatePdf(cert);
}
```

The `@AuthenticationPrincipal User user` injects the currently authenticated user (extracted from the JWT by `JwtAuthenticationFilter`). The method verifies ownership at line 179: `cert.getStudentId().equals(student.getId())` — a student can only download PDFs for their own certificates. If the certificate belongs to a different student, a 403 Forbidden response is returned.

### STEP 3 — PDF is generated

**File:** `backend/src/main/java/com/eduauth/service/CertificateService.java`

The `generatePdf()` method starts at line 78. The PDF generation uses **iText 7**, a Java library for creating and manipulating PDF documents. The **ZXing** library generates the QR code.

**Page setup (lines 80–87):**

```java
ByteArrayOutputStream baos = new ByteArrayOutputStream();
PdfWriter writer = new PdfWriter(baos);
PdfDocument pdfDoc = new PdfDocument(writer);
pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
Document doc = new Document(pdfDoc);
doc.setMargins(40, 40, 40, 40);
```

The PDF is written to a `ByteArrayOutputStream` (in memory, not to a file). The page size is A4 Landscape. Margins are 40 points on all sides.

**Border drawing (lines 90–92):**

```java
drawBorder(pdfDoc, NAVY, 6f, 20f);   // Outer border — navy, 6pt thick, 20pt from edge
drawBorder(pdfDoc, GOLD, 1.5f, 30f); // Inner border — gold, 1.5pt thick, 30pt from edge
```

The `drawBorder()` helper at lines 276–290 uses `PdfCanvas` to draw rectangles directly on the page.

**Font setup (lines 94–103):**

```java
PdfFont serifBold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
PdfFont serifItalic = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
PdfFont serif = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
PdfFont helvetica = PdfFontFactory.createFont(StandardFonts.HELVETICA);
PdfFont helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
```

These are the 14 standard PDF fonts available in every PDF viewer — no embedding required.

**University name (lines 129–139):**

```java
doc.add(new Paragraph(institutionName)
        .setFont(serifBold)
        .setFontSize(28)
        .setFontColor(NAVY)
        .setTextAlignment(TextAlignment.CENTER)
        .setCharacterSpacing(1.5f)
        .setMarginTop(20));
```

**"Certificate of Achievement" title (lines 142–147)** in italic serif, gold color, 36pt.

**Body text (lines 150–166):** Constructs the sentence "This is to certify that **[Student Name]** has successfully completed the requirements for the degree of **[Degree] [Major]**." The student name and degree are bold.

**QR code generation (lines 198–199):**

```java
String shareLink = buildShareLink(cert);
byte[] qrBytes = generateQrCode(shareLink, 120);
```

The `buildShareLink()` method at lines 63–70 constructs the verification URL:

```java
public String buildShareLink(Certificate cert) {
    String dob = cert.getStudent().getDateOfBirth().toString();  // yyyy-MM-dd
    String encryptedDob = encryptionService.encryptDOB(dob);
    return FRONTEND_BASE + "/verify?s=" + cert.getSerial() + "&v=" + encryptedDob;
}
```

The `generateQrCode()` helper at lines 264–273 uses ZXing:

```java
private byte[] generateQrCode(String content, int size) throws Exception {
    QRCodeWriter writer = new QRCodeWriter();
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, 1);
    BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
    BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "PNG", out);
    return out.toByteArray();
}
```

The QR code encodes the full share link URL. When scanned, it opens the verification page with the serial and encrypted DOB pre-filled.

The QR code PNG bytes are embedded into the PDF at lines 211–212:

```java
Image qrImage = new Image(ImageDataFactory.create(qrBytes)).setWidth(100).setHeight(100);
leftCell.add(qrImage);
```

### STEP 4 — PDF bytes returned

At lines 240–247:

```java
byte[] pdf = baos.toByteArray();

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_PDF);
headers.setContentDispositionFormData("attachment", cert.getSerial() + ".pdf");
headers.setContentLength(pdf.length);

return ResponseEntity.ok().headers(headers).body(pdf);
```

`Content-Type: application/pdf` tells the browser the response is a PDF file. `Content-Disposition: form-data; name="attachment"; filename="BSC-26-000001M.pdf"` tells the browser to treat it as a downloadable file with the serial number as the filename. The raw PDF bytes are sent as the response body.

---

## 5. Where and How Encryption Works

### 5a. Password Hashing (BCrypt)

**File:** `backend/src/main/java/com/eduauth/service/AuthService.java`

At registration, the password is hashed at line 100:

```java
user.setPassword(passwordEncoder.encode(regData.getPassword()));
```

At login, the password is verified at line 183:

```java
if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
```

The `passwordEncoder` bean is a `BCryptPasswordEncoder`, defined in `SecurityConfig.java` lines 52–55:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt is a password hashing algorithm designed to be intentionally slow, making brute-force attacks impractical. Unlike SHA-256, which is designed to be fast (good for checksums, bad for passwords), BCrypt has a configurable cost factor that controls how many rounds of hashing are performed.

A BCrypt hash looks like: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`. The `$2a$` prefix identifies the BCrypt version. The `$10$` means the cost factor is 10 (which means 2^10 = 1024 rounds of the key expansion). The next 22 characters are the base64-encoded salt. The remaining characters are the hash itself.

Each time `encode()` is called, BCrypt generates a new random salt. This means hashing the same password twice produces two completely different hash strings. But `matches()` can still verify them because the salt is embedded in the hash string — BCrypt extracts the salt, re-hashes the candidate password with the same salt and cost, and compares.

### 5b. NID Hashing (SHA-256)

**File:** `backend/src/main/java/com/eduauth/util/HashUtil.java`

The `sha256()` method at lines 26–42:

```java
public static String sha256(String input) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexBuilder = new StringBuilder(hashBytes.length * 2);
        for (byte b : hashBytes) {
            hexBuilder.append(String.format("%02x", b));
        }
        return hexBuilder.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 algorithm not available", e);
    }
}
```

Line 28: `MessageDigest.getInstance("SHA-256")` gets the SHA-256 algorithm implementation from Java's security provider. SHA-256 is mandated by the Java specification, so this never throws in practice.

Line 29: `digest.digest(...)` hashes the input string's UTF-8 bytes and returns a 32-byte (256-bit) array.

Lines 31–34: The byte array is converted to a lowercase hexadecimal string. Each byte becomes two hex digits (`%02x` means zero-padded, 2-digit, lowercase hexadecimal). For example, the byte `0x0A` becomes `"0a"`. The final output is always 64 characters long.

SHA-256 is a **one-way** hash. It cannot be reversed — there is no mathematical function to recover the original NID from its hash. This is the whole point: we store the hash in the `nid_hash` column so that if the database is breached, the attacker gets hash strings that are useless without the original NIDs.

To verify whether a given NID belongs to a student, we hash the candidate NID and compare the result to the stored hash. If the hashes match, the NIDs match. This is done in the codebase wherever NID comparison is needed: hash the input, compare with the stored `nid_hash`.

### 5c. DOB Encryption for Share Links

**File:** `backend/src/main/java/com/eduauth/service/EncryptionService.java`

**Encryption — `encryptDOB()` at lines 43–64:**

```java
public String encryptDOB(String dob) {
    Cipher cipher = Cipher.getInstance(ALGORITHM);      // AES/CBC/PKCS5Padding
    byte[] iv = new byte[IV_LENGTH];                     // 16 bytes
    new java.security.SecureRandom().nextBytes(iv);      // Fill with random bytes
    IvParameterSpec ivSpec = new IvParameterSpec(iv);

    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
    byte[] encrypted = cipher.doFinal(dob.getBytes(StandardCharsets.UTF_8));

    byte[] combined = new byte[IV_LENGTH + encrypted.length];
    System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
    System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
}
```

The algorithm is AES-256 in CBC (Cipher Block Chaining) mode with PKCS5 padding. A random 16-byte IV (Initialization Vector) is generated for every encryption call, ensuring that encrypting the same DOB twice produces different ciphertext. The IV is prepended to the ciphertext. The combined bytes are Base64-encoded using the URL-safe variant (`-` and `_` instead of `+` and `/`) without padding (`=` characters omitted), making the output safe to embed in a URL.

**Decryption — `decryptDOB()` at lines 71–97:** Already covered in Section 3, Step 4.

**Why URL-safe Base64?** Standard Base64 uses `+`, `/`, and `=`. In URLs, `+` is interpreted as a space, `/` is a path separator, and `=` is a parameter delimiter. URL-safe Base64 replaces these with `-`, `_`, and omits padding, so the encrypted token can be placed directly in a URL query parameter without further encoding.

**What a share link looks like:**

Before encryption:
```
DOB = "2000-01-15"
Serial = "BSC-26-000001M"
```

After encryption:
```
http://localhost:5173/verify?s=BSC-26-000001M&v=yK3bT7dPxZkR9wMn2QhLfA5j8cXvE1aU4sGoBi6Hp
```

The `v` parameter is the AES-encrypted, URL-safe Base64-encoded date of birth.

**Why is this encryption reversible but BCrypt is not?** BCrypt is a hash — it is a one-way function by design. You can verify a password against a hash, but you cannot recover the password from the hash. AES encryption is two-way — with the correct key, you can both encrypt and decrypt. We need reversibility here because the backend must recover the original date of birth from the share link token to compare it against the database. Hashing would make this impossible.

**What happens if the key changes?** The encryption key is derived from the JWT secret in `application.properties`. If you change the JWT secret, all previously generated share links become invalid because the new key cannot decrypt tokens encrypted with the old key. The `decryptDOB()` method would return `null` and the verification would fail with "Invalid or expired verification link."

### 5d. JWT Signing

**File:** `backend/src/main/java/com/eduauth/service/JwtService.java`

A JWT has three parts: `header.payload.signature`.

The **header** contains metadata: `{"alg":"HS256"}` — this tells the verifier which algorithm was used to sign the token. It is base64url-encoded.

The **payload** contains the claims (user data): `{"sub":"5","email":"student@test.com","role":"student","iat":1725100000,"exp":1725704800}`. It is also base64url-encoded.

The **signature** is computed by applying HMAC-SHA256 to `base64url(header) + "." + base64url(payload)` using the secret key. This is what `signWith(secretKey, Jwts.SIG.HS256)` does at line 53.

HMAC-SHA256 is a keyed hash function. "Signing" means: "I used my secret key to produce a hash of the header and payload. Anyone who has the same secret key can recompute this hash and verify that the token has not been modified."

**Can someone decode the payload without the secret key?** Yes. The payload is only base64-encoded, not encrypted. Anyone can decode it and read the email, role, and user ID. This is why you must never put sensitive information (passwords, credit card numbers) in a JWT.

**Can someone modify the payload without detection?** No. If an attacker changes `"role":"student"` to `"role":"admin"` in the payload and re-encodes it, the signature would no longer match. When the server calls `parseClaims()` at lines 89–95, the `verifyWith(secretKey)` step recomputes the HMAC of the received header+payload and compares it to the received signature. If they do not match, a `JwtException` is thrown and `validateToken()` returns `false`.

This is why we call it **signed**, not encrypted. The data is visible to everyone, but tampering is detectable.

---

## 6. How Emails are Sent (OTP and Approvals)

The system sends two types of emails: OTPs for email verification during registration, and approval notifications when an admin approves a pending account. All email sending logic is centralized in the `EmailService` and uses Spring's `JavaMailSender`.

### Step 1 — The EmailService Wrapper

**File:** `backend/src/main/java/com/eduauth/service/EmailService.java`

This service is a Spring `@Service` that wraps `JavaMailSender`. It defines two methods: `sendOtpEmail()` and `sendApprovalEmail()`. 

Both methods follow the same pattern:
1. They create a `SimpleMailMessage`.
2. They set the recipient (`message.setTo(toEmail)`), subject, and the body text.
3. The sender email address is injected from `application.properties` using `@Value("${app.mail.from}")`.
4. Finally, `mailSender.send(message)` dispatches the email via SMTP.

### Step 2 — Triggering OTP Emails (Registration)

**File:** `backend/src/main/java/com/eduauth/service/AuthService.java`

When a new user submits the registration form, the `register()` method processes the request. It generates a random 6-digit OTP and saves it as a hashed value in the `pending_registrations` table.

At line 67, it triggers the email:
```java
emailService.sendOtpEmail(request.getEmail(), otp, name);
```
A similar call happens in the `resendOtp()` method at line 175 if the user requests a new verification code.

### Step 3 — Triggering Approval Emails (Admin Action)

**File:** `backend/src/main/java/com/eduauth/controller/admin/AdminUserController.java`

New university and verifier accounts remain in an unapproved state (`is_approved = false`) until an administrator reviews them. When an admin clicks "Approve" on the dashboard, the frontend calls the `POST /api/admin/users/{id}/approve` endpoint.

Inside `AdminUserController.approveUser()`, the user's `is_approved` flag is set to `true`. Then, at line 90, the system notifies the user:
```java
emailService.sendApprovalEmail(user.getEmail(), name);
```
This lets the user know they can now log in successfully.

---

## 7. How Spring Security Decides Who Can Access What

**File:** `backend/src/main/java/com/eduauth/config/SecurityConfig.java`
**File:** `backend/src/main/java/com/eduauth/config/JwtAuthenticationFilter.java`

Every HTTP request to Spring Boot passes through a **filter chain** — an ordered sequence of filters that process the request before it reaches a controller. Spring Security inserts its own filters into this chain.

### The Filter Chain Order

The relevant order is:

1. **CORS filter** — handles cross-origin preflight (`OPTIONS`) requests
2. **JwtAuthenticationFilter** — our custom filter, added *before* `UsernamePasswordAuthenticationFilter` at line 130 of `SecurityConfig.java`
3. **Spring Security's authorization filter** — checks the route rules defined in `authorizeHttpRequests`

### JwtAuthenticationFilter in Detail

The filter at lines 43–103 of `JwtAuthenticationFilter.java` runs these steps:

**Step 1 (line 50):** Read the `Authorization` header from the request.

**Step 2 (lines 53–56):** If the header is missing or does not start with `"Bearer "`, skip authentication entirely and pass the request along. This means public endpoints work without any token.

**Step 3 (line 59):** Extract the raw token by stripping the `"Bearer "` prefix: `authHeader.substring(7)`.

**Step 4 (lines 64–68):** Check if the token is blacklisted. When a user logs out, their token is added to the blacklist (`TokenBlacklistService`). If the token is found in the blacklist, authentication is skipped — the request continues as unauthenticated.

**Step 5 (lines 71–74):** Validate the token using `jwtService.validateToken(token)`. This calls `parseClaims(token)` which verifies the signature and checks the expiration date. If the token is malformed, expired, or has an invalid signature, authentication is skipped.

**Step 6 (line 77):** Extract the email from the token: `jwtService.extractEmail(token)`.

**Step 7 (lines 80–81):** Load the full `UserDetails` from the database using `userDetailsService.loadUserByUsername(email)`. This is only done if the `SecurityContextHolder` does not already have an authentication object (preventing redundant database queries).

**Step 8 (lines 84–91):** Build a `UsernamePasswordAuthenticationToken` with the `UserDetails` object, null credentials (we do not need the password again — the JWT already proved identity), and the user's authorities (roles).

**Step 9 (line 94):** Set this authentication object in `SecurityContextHolder`:

```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

`SecurityContextHolder` is a thread-local container that Spring Security uses to store the identity of the currently authenticated user for the duration of the request. After this line, any code in the same request can call `SecurityContextHolder.getContext().getAuthentication()` to get the user, and annotations like `@AuthenticationPrincipal User user` work.

**Step 10 (line 102):** The request continues down the filter chain to the authorization rules.

### Authorization Rules in SecurityConfig

The `authorizeHttpRequests` block at lines 87–121 defines who can access what:

```java
.requestMatchers("/api/student/**").hasRole("STUDENT")
.requestMatchers("/api/university/**").hasRole("UNIVERSITY")
.requestMatchers("/api/verifier/**").hasRole("VERIFIER")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/**").authenticated()
```

**How does `hasRole("STUDENT")` work with the `ROLE_` prefix?** In the `User` model (`User.java`, line 112):

```java
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
}
```

If the user's role is `"student"`, their authority becomes `"ROLE_STUDENT"`. Spring Security's `hasRole("STUDENT")` internally prepends `"ROLE_"` to the argument, making it `"ROLE_STUDENT"`, and checks if the user's authorities contain it.

**What happens when a student tries to access `/api/admin/certificates`?** The `JwtAuthenticationFilter` successfully authenticates the student (their JWT is valid). But when Spring Security checks the authorization rule `.requestMatchers("/api/admin/**").hasRole("ADMIN")`, the student's authority is `ROLE_STUDENT`, not `ROLE_ADMIN`. The request is denied. The `accessDeniedHandler` at lines 156–161 returns a JSON response:

```json
{"success": false, "message": "Access denied"}
```

with HTTP status 403 (Forbidden).

**What about the 401 handler?** If a request to a protected endpoint has no valid authentication at all (no token, expired token, blacklisted token), the `unauthorizedEntryPoint` at lines 144–150 returns:

```json
{"success": false, "message": "Unauthorized"}
```

with HTTP status 401. Both handlers return JSON instead of HTML redirects — this is critical for an SPA because the frontend needs machine-readable error responses, not login page HTML.

---

## 8. How the Frontend Knows What to Show (Routing + Auth Guard)

### React Router

**File:** `frontend/src/App.jsx`

The entire app is wrapped in `<BrowserRouter>` at line 80. React Router matches the current URL against the `<Route>` definitions and renders the matching component. For example, when the URL is `/student/dashboard`, React Router matches line 131:

```jsx
<Route path="/student/dashboard" element={<StudentDashboard />} />
```

All page components are lazy-loaded using `React.lazy()` (lines 20–76), which means they are only downloaded when first navigated to. While loading, the `<Suspense fallback={<PageLoader />}>` at line 97 shows a loading spinner.

### ProtectedRoute

**File:** `frontend/src/components/auth/ProtectedRoute.jsx`

The `ProtectedRoute` component wraps groups of routes that require authentication and specific roles:

```jsx
<Route element={<ProtectedRoute allowedRoles={['student']} />}>
  <Route path="/student/dashboard" element={<StudentDashboard />} />
  <Route path="/student/certificates" element={<StudentCertificates />} />
  ...
</Route>
```

The `ProtectedRoute` acts as a layout route. When React Router tries to render a nested route, it first renders `ProtectedRoute`, which checks authentication state.

Three checks happen in order:

1. **Loading state (line 8):** If `AuthContext` is still initializing (reading from `localStorage`), show a spinner. This prevents a flash of the login page on hard refresh.

2. **Not authenticated (line 16):** If `user` is null, redirect to `/login`. The `replace` prop means the current URL is replaced in browser history, so clicking "Back" does not return to the protected page.

3. **Wrong role (line 20):** If `allowedRoles` is specified and the user's role is not in the list, redirect to `/`. This prevents a student from manually typing `/admin/dashboard` in the URL bar.

If all checks pass, `<Outlet />` renders the nested child route.

### AuthContext

**File:** `frontend/src/contexts/AuthContext.jsx`

`AuthContext` is a React context that provides authentication state and methods to every component in the app. The `AuthProvider` (line 7) wraps the entire component tree in `App.jsx` (line 82).

The context value (lines 62–75) provides:

- `user` — the current user object or `null`
- `loading` — true while reading from localStorage on startup
- `login(credentials)` — calls authService.login, updates state
- `logout()` — calls authService.logout, clears state
- `refreshUser()` — fetches current user from the API and updates state
- `isAuthenticated` — boolean computed from `!!user`
- `isStudent`, `isUniversity`, `isVerifier`, `isAdmin` — role convenience booleans

Any component can call `const { user, isAdmin, login, logout } = useAuth()` to access these values.

The `useAuth()` hook at lines 80–86 enforces that it is only used inside an `AuthProvider`:

```javascript
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

### Token Expiry Handling

**File:** `frontend/src/services/api.js`

The response interceptor at lines 28–42 handles expired tokens:

```javascript
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = error.config?.url?.includes('/auth/login') || ...;
    if (error.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

When any API call returns a 401 (Unauthorized) — which happens when the JWT has expired or is invalid — the interceptor clears `localStorage` and redirects the user to the login page. The `isAuthRequest` check prevents this from triggering on login/register failures (which also return 401 for wrong credentials but should not cause a redirect).

---

## 9. Common Professor Questions — Answered

**Q: Why did you use JWT instead of sessions?**

I chose JWT because the architecture is fully stateless. The Spring Boot backend at line 83–84 of `SecurityConfig.java` sets `SessionCreationPolicy.STATELESS`, meaning no `HttpSession` is ever created. Every request carries its own proof of identity in the `Authorization` header. This eliminates the need for session storage on the server (no in-memory map, no Redis, no database session table). If I needed to scale to multiple backend instances behind a load balancer, each instance can independently verify a JWT using the shared secret key without needing to synchronize session data. Sessions would require sticky sessions or a shared session store, which adds complexity and a potential single point of failure.

**Q: Where is the token stored and is that secure?**

The token is stored in `localStorage` under the key `'token'` — this happens in `authService.js` at line 12: `localStorage.setItem('token', response.data.token)`. localStorage is accessible by any JavaScript running on the same origin. This means it is vulnerable to XSS (Cross-Site Scripting) attacks — if an attacker can inject JavaScript into the page, they can read the token. We mitigate this by: (1) React automatically escapes user input in JSX, preventing most XSS vectors; (2) the app does not use `dangerouslySetInnerHTML`; (3) CORS configuration in `CorsConfig.java` restricts which origins can make requests to the backend. The alternative — storing the token in an HttpOnly cookie — would protect against XSS but would require CSRF protection, which conflicts with the stateless design.

**Q: What happens if someone intercepts the JWT token?**

If an attacker intercepts the token (e.g., through a man-in-the-middle attack on HTTP), they can impersonate the user for the lifetime of the token (7 days, configured at `application.properties` line 19: `jwt.expiration-ms=604800000`). To mitigate this: (1) in production, HTTPS must be enforced so tokens are encrypted in transit; (2) the logout mechanism calls `tokenBlacklistService.blacklist(token)` in `AuthService.java` line 230, which adds the token to a blacklist checked by `JwtAuthenticationFilter` at line 64; (3) the token expiry limits the window of exploitation.

**Q: Why is the NID hashed but DOB encrypted?**

The NID is hashed because we never need to recover the original value. We only need to check "does this NID match the one on file?" which hashing supports. SHA-256 in `HashUtil.sha256()` produces a one-way, irreversible digest. Even if the database is breached, the original NID numbers cannot be recovered.

The DOB is encrypted because we *do* need to recover the original value. The share link embeds the encrypted DOB, and the backend must decrypt it to compare with the database. `EncryptionService.decryptDOB()` reverses the AES-256 encryption to get the original date string.

**Q: Why does a wrong DOB return "not found" instead of "incorrect DOB"?**

Looking at the actual code in `VerifyController.java` lines 167–171, the response message is "Date of birth does not match our records" (not "not found"). This is an information leakage trade-off. In the share link flow, this scenario should not occur because the DOB is embedded in the encrypted token. In the manual verification flow, the system returns a specific message so the user knows their serial was correct but the DOB was wrong. If complete privacy were required, we could return a generic "verification failed" for both cases to avoid revealing whether a serial exists.

**Q: What prevents someone from generating a fake certificate serial?**

The serial includes a checksum character calculated by `SerialGeneratorService.calculateChecksum()`. The checksum is a single character from a 31-character set, computed from the sum of ASCII values of the prefix, year, and sequence digits, modulo 31. While this provides basic tamper detection (catching typos and random guesses), it is not cryptographically strong — the algorithm is simple enough that someone who knows it could compute a valid checksum. The real security is that even with a valid-format serial, the certificate must exist in the database, and the DOB must match.

**Q: What does the checksum character in the serial actually prove?**

It proves that the serial was not mistyped. For example, if someone types `BSC-26-000001N` instead of `BSC-26-000001M`, the checksum validation in `validateChecksum()` fails immediately, and the system returns "Invalid certificate serial number format" without even querying the database. This reduces unnecessary database queries and provides instant feedback. It does not prove authenticity — only the database lookup and DOB verification do that.

**Q: What is CORS and why did you need to configure it?**

CORS (Cross-Origin Resource Sharing) is a browser security mechanism that blocks web pages from making requests to a different origin (protocol + domain + port). The React frontend runs on `http://localhost:5173` and the Spring Boot backend on `http://localhost:8080`. These are different origins (different ports). Without CORS configuration, the browser would block all API requests. In `CorsConfig.java` (lines 22–29), I configured the backend to explicitly allow requests from the frontend URL:

```java
registry.addMapping("/**")
        .allowedOrigins(frontendUrl)    // "http://localhost:5173"
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Authorization")
        .allowCredentials(false)
        .maxAge(3600);
```

`allowCredentials(false)` is deliberate — we use Bearer tokens, not cookies.

**Q: What is the difference between authentication and authorization in your system?**

Authentication is "who are you?" — handled by `JwtAuthenticationFilter`, which validates the JWT and identifies the user. Authorization is "what are you allowed to do?" — handled by the `authorizeHttpRequests` rules in `SecurityConfig.java` (lines 87–121) and `@PreAuthorize` annotations on controllers. A student is *authenticated* (we know who they are), but they are not *authorized* to access `/api/admin/**` (they do not have the `ROLE_ADMIN` authority). Authentication happens in the filter. Authorization happens after, in the security rules.

**Q: What happens when two universities issue certificates at the exact same millisecond?**

The serial number is composed of a prefix (degree abbreviation), year, and a 6-digit sequence number. The sequence number is typically generated by the database using an auto-increment counter or a synchronized sequence. Because the serial is a unique column (`@Column(unique = true)` in `Certificate.java` at line 49), the database enforces uniqueness. If two transactions try to insert the same serial simultaneously, the database's unique constraint causes one of them to fail with a constraint violation exception. The application would need to handle this by retrying with the next sequence number.

**Q: Why can a student have only one active enrollment at a time?**

This is a business rule enforced by the enrollment system. A student at a Bangladeshi university is typically enrolled in one program at one institution at a time. The enrollment links a student to a specific institution, program, and batch. If a student transfers, the previous enrollment is closed and a new one is created. This prevents conflicts in certificate issuance — each certificate is linked to a specific enrollment, so there is no ambiguity about which program or institution it belongs to.

**Q: What is bcrypt and why not SHA-256 for passwords?**

BCrypt is an adaptive password hashing function based on the Blowfish cipher. The `BCryptPasswordEncoder` used at `SecurityConfig.java` line 54 implements it. BCrypt is preferred over SHA-256 for passwords because: (1) BCrypt is intentionally slow — the cost factor (`$10$` = 1024 rounds) makes brute-force attacks take orders of magnitude longer; (2) BCrypt automatically generates and embeds a random salt, so two users with the same password get different hashes; (3) SHA-256 is designed to be fast (useful for data integrity checks), which is exactly what you *don't* want for password hashing — a GPU can compute billions of SHA-256 hashes per second. I use SHA-256 for NID hashing (`HashUtil.sha256()`) because speed is not a concern there — the NID is long and random enough that brute-force is impractical.

**Q: What does stateless session mean in Spring Security?**

At `SecurityConfig.java` line 83–84:

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

This tells Spring Security to never create or use an `HttpSession`. Without this, Spring would create a server-side session for each authenticated user, storing their authentication state in memory. With `STATELESS`, every request must carry its own credentials (the JWT). The server does not remember anything between requests. `SecurityContextHolder` is populated fresh for every request by the `JwtAuthenticationFilter` and cleared when the request completes.

**Q: How does the QR code link back to the verification page?**

The QR code encodes the full share link URL. In `CertificateService.java` at line 198:

```java
String shareLink = buildShareLink(cert);
```

The `buildShareLink()` method (line 63) builds `http://localhost:5173/verify?s=BSC-26-000001M&v=encryptedDOB`. This URL is encoded into the QR code by ZXing at line 199. When someone scans the QR code with their phone camera, the browser opens this URL, which loads the React app, matches the `/verify` route to the `VerifyCertificate` component, reads the `s` and `v` parameters, and automatically triggers the verification API call.

**Q: What would happen if you changed the JWT secret key?**

Three things break immediately: (1) All existing JWTs become invalid — `JwtService.parseClaims()` at line 91 calls `verifyWith(secretKey)`, which recomputes the HMAC signature with the new key, and it will not match the old signature, causing `validateToken()` to return `false`. All currently logged-in users are effectively logged out. (2) All existing share link tokens become undecryptable — `EncryptionService` derives its AES key from the JWT secret at line 33. With a new secret, `decryptDOB()` cannot decrypt tokens encrypted with the old key. All previously generated share links stop working. (3) The token blacklist becomes irrelevant — since all old tokens are now invalid regardless, the blacklist entries for those tokens are moot.

---

*End of documentation.*
