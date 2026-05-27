# Account Service — Kotlin + Spring Boot

A production-inspired REST API simulating the authentication and payroll subsystem of a corporate account management platform, built as part of the Hyperskill Backend Developer track. The service handles employee registration, authentication, salary management, role-based access control, password security policies, and a complete security event audit log.

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.1.0 (JVM) |
| Framework | Spring Boot 3.x + Jakarta EE |
| Build Tool | Gradle (Groovy DSL) |
| ORM | Spring Data JPA + Hibernate |
| Database | H2 (file-based, development) |
| Security | Spring Security 6.x — HTTP Basic Auth + RBAC |
| Testing | JUnit 5 + MockMvc + spring-security-test |

---

## Architecture

The service follows a strict four-layer architecture with clear separation of concerns:

```
HTTP Request
    │
    ▼
Security Filter Chain        ← Spring Security: authenticates credentials, enforces role rules
    │
    ▼
Controller Layer             ← HTTP concerns: request parsing, response shaping
    │
    ▼
Service Layer                ← Business logic: validation, hashing, role management
    │
    ▼
Repository Layer             ← Spring Data JPA: zero hand-written SQL
```

---

## Feature Set — Stage Progression

The project was built incrementally across seven stages, each introducing a new layer of backend engineering:

### Stage 001 — REST API Foundation
Implemented a stateless `POST /api/auth/signup` endpoint with manual field validation, typed error responses, and global exception handling via `@RestControllerAdvice`. Key discovery: `javax.validation` annotations compile as `RuntimeInvisible` under Kotlin 2.1.0, confirmed via `javap` bytecode inspection — resolved by implementing validation manually using `isNullOrBlank()` and `Regex`.

### Stage 002 — Authentication
Introduced user persistence with Spring Data JPA and H2, BCrypt password hashing, and a stateless HTTP Basic Auth security filter chain backed by a custom `UserDetailsService`. The `GET /api/empl/payment/` endpoint is protected — unauthenticated requests return a JSON `401`, not a browser redirect.

### Stage 003 — Password Change Policy
Added `POST /api/auth/changepass` with a 12-character minimum length requirement and reuse prevention. New passwords are compared against the current BCrypt hash using `encoder.matches()` — the original plaintext is never stored or compared directly.

### Stage 004 — Payroll Management
Implemented batch payroll upload (`POST /api/acct/payments`) as a fully `@Transactional` operation — any validation failure in the batch rolls back all prior inserts atomically. Salary is stored as `Long` in cents throughout to avoid floating-point rounding errors. The employee-facing endpoint (`GET /api/empl/payment`) returns salary formatted as `X dollar(s) Y cent(s)`, sorted by period descending.

### Stage 005 — Role-Based Access Control
Implemented a three-role model (User, Accountant, Administrator) with `@PreAuthorize` method-level security and a deny-by-default filter chain. The first registered user is automatically assigned `ROLE_ADMINISTRATOR`. Administrative and business roles are mutually exclusive — a user cannot hold roles from both groups simultaneously. Admin management endpoints cover role assignment, user deletion, and user listing.

### Stage 006 — Security Event Audit Log
Added a persistent audit log covering all security-relevant operations: `LOGIN_FAILED`, `ACCESS_DENIED`, `CREATE_USER`, `CHANGE_PASSWORD`, `GRANT_ROLE`, `REMOVE_ROLE`, `DELETE_USER`, `LOCK_USER`, `UNLOCK_USER`, and `BRUTE_FORCE`. Brute-force protection locks an account after five consecutive failed login attempts. Accounts can be manually unlocked via `PUT /api/admin/user/access`.

### Stage 007 — Full Integration
All stages integrated into a single cohesive application. All Hyperskill grader tests pass across authentication, payroll, roles, and security event suites.

---

## API Reference

| Method | Endpoint | Auth | Role |
|--------|----------|------|------|
| `POST` | `/api/auth/signup` | No | Public |
| `POST` | `/api/auth/changepass` | Yes | User, Accountant |
| `GET` | `/api/empl/payment` | Yes | User |
| `POST` | `/api/acct/payments` | Yes | Accountant |
| `PUT` | `/api/acct/payments` | Yes | Accountant |
| `GET` | `/api/admin/user` | Yes | Administrator |
| `DELETE` | `/api/admin/user/{email}` | Yes | Administrator |
| `PUT` | `/api/admin/user/role` | Yes | Administrator |
| `PUT` | `/api/admin/user/access` | Yes | Administrator |
| `GET` | `/api/security/events` | Yes | Auditor |

All error responses follow a consistent contract:

```json
{
  "timestamp": "2026-04-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "<specific reason>",
  "path": "/api/..."
}
```

---

## Security Design

**Password storage** — BCrypt with a work factor of 10. Passwords are never returned in API responses, never logged in plaintext, and never compared directly — only via `BCryptPasswordEncoder.matches()`.

**Stateless sessions** — `SessionCreationPolicy.STATELESS` is set explicitly. No `HttpSession` is created or read, which eliminates session fixation attacks and allows horizontal scaling without sticky sessions.

**CSRF disabled** — CSRF exploits session cookies. With no sessions, there is no attack surface to protect.

**Deny by default** — The security filter chain closes with `.anyRequest().denyAll()`. Every accessible endpoint is explicitly opened; nothing is accessible by omission.

**Role group separation** — Administrative roles (Administrator) and business roles (User, Accountant) are mutually exclusive. A user cannot simultaneously hold roles from both groups.

**Brute-force protection** — Five consecutive authentication failures trigger an automatic account lock. All lock and unlock events are recorded in the audit log.

**Vague error messages** — The generic exception handler returns a non-descriptive message for all `500` responses, preventing internal stack traces from leaking to API clients.

---

## Data Design

**`users` table** — Email is stored normalized to lowercase (RFC 5321: email local parts are case-insensitive). A `UNIQUE` constraint on the email column provides concurrency safety beyond the application-level duplicate check. The table is explicitly named `users` — `user` is a reserved keyword in H2 and PostgreSQL.

**`payments` table** — Salary is stored as `Long` in cents. Floating-point arithmetic is never used for monetary values. A composite unique constraint on `(employee, period)` is enforced at the database level.

**`security_events` table** — Append-only audit log. Events are never updated or deleted.

---

## Testing

The project follows a two-tier testing strategy:

**Unit tests** — `AuthController.validate()` is tested in complete isolation with no Spring context. The controller is instantiated directly with a mocked `UserService`. These tests run in milliseconds.

**Integration tests** — The full Spring context is booted including the Security filter chain and H2 database. `MockMvc` fires HTTP requests through the complete middleware stack. Authentication is tested using `httpBasic()` from `spring-security-test`, which exercises the real `BasicAuthenticationFilter` — not a mock bypass.

Database state is reset between tests with `@BeforeEach userRepository.deleteAll()` rather than `@DirtiesContext`, avoiding the 2–5 second context reload penalty per test class.

---

## Project Structure

```
src/main/kotlin/account/
├── controller/
│   ├── AuthController.kt          # POST /api/auth/signup, POST /api/auth/changepass
│   ├── PaymentController.kt       # GET /api/empl/payment, POST/PUT /api/acct/payments
│   ├── AdminController.kt         # /api/admin/** endpoints
│   └── SecurityController.kt      # GET /api/security/events
├── dto/                           # Request/response/error DTOs
├── entity/                        # JPA entities: UserEntity, PaymentEntity, SecurityEvent
├── exception/                     # GlobalExceptionHandler, custom exceptions
├── repository/                    # Spring Data JPA repositories
├── security/                      # SecurityConfig, RestAuthEntryPoint, AccessDeniedHandler
└── service/                       # UserService (implements UserDetailsService), SecurityEventService
```

---

## Known Limitations & Production Path

This service was built for a learning environment. The gaps between the current implementation and a production-grade deployment are documented explicitly:

| Area | Current | Production |
|------|---------|------------|
| Database | H2 file-based | PostgreSQL with Flyway migrations |
| Schema management | `ddl-auto=update` | Versioned migration scripts (`V1__`, `V2__`, ...) |
| Authentication | HTTP Basic Auth | OAuth2 / JWT Bearer tokens |
| Secrets | `application.properties` | HashiCorp Vault / AWS Secrets Manager |
| Rate limiting | None | API gateway with per-IP limits on auth endpoints |
| Password hashing | BCrypt, work factor 10 | Argon2id or BCrypt factor 12+ |
| Observability | Spring Actuator only | Structured logs → ELK, Micrometer → Prometheus/Grafana |
| TLS | None | TLS termination at load balancer, HSTS enforced |

---

*Built by Oliver (Christian) Jaramillo — Hyperskill Kotlin Backend Track, April 2026*
