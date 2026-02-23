# OrthoWatch — Progress Log

## Phase 1: Project Setup & Foundation ✅
**Status**: Fully Verified. Backend (Spring Boot 3.2 + JDK 21), Frontend (Vite + React), and Infrastructure (Docker) are set up and operational.

### What was built

#### Backend (`/backend`)
- **Spring Boot 3.2.2 / Java 21** project with full `pom.xml`
  - All dependencies: Web, JPA, Redis, Security, Validation, Actuator, Quartz, Flyway, JWT (jjwt 0.12.3), Lombok, MapStruct, SpringDoc, Sentry, Testcontainers, REST Assured
  - Spotless plugin (Google Java Style)
  - Lombok + MapStruct annotation processors
  - hypersistence-utils-hibernate-63 for JSONB/array types
- **Maven Wrapper** (mvnw / mvnw.cmd + .mvn/wrapper/)
- **Application config**: `application.yml`, `application-dev.yml`, `application-prod.yml` for profile-specific settings
- **`.env.example`** with all essential environment variables documented (JWT secrets, API URLs, Database credentials)

#### Database
- **Flyway migrations**:
  - `V1__initial_schema.sql` — all 13 tables with CHECK constraints, FK, indexes, immutability rules on `clinical_audit_log`
  - `V2__seed_templates.sql` — 3 recovery templates (TKR/THR/ACL) with JSONB configs, 5 risk rules, hospital settings
  - `V3__seed_admin_user.sql` — admin user BCrypt password update

#### JPA Entities (13)
`User`, `Patient`, `RecoveryTemplate`, `Episode`, `DailyResponse`, `RiskScore`, `Alert`, `WoundImage`, `ConsentLog`, `ClinicalAuditLog`, `RiskRule`, `Session`, `HospitalSettings`

#### Spring Data JPA Repositories (13)
One repository per entity with domain-specific query methods.

#### Infrastructure
- **`docker-compose.yml`** — PostgreSQL 16 + Redis 7 with health checks
- **`.gitignore`** — Java/Maven, env files, IDE, OS, Node

#### Frontend (`/frontend`)
- **Vite 5 + React 18 + TypeScript 5.3** project scaffolded
- All dependencies installed: React Query, Zustand, React Router, Recharts, Lucide, React Hook Form, Zod, Axios, date-fns, clsx, tailwind-merge
- **Tailwind CSS 3.4** configured with full OrthoWatch design system:
  - Color palettes: primary, success, warning, danger, risk, neutral
  - Inter font, custom spacing, animations
  - Component utilities: cards, buttons, form inputs, risk badges, sidebar links
- **`vite.config.ts`** with `@` path alias and `/api` proxy to backend
- **`App.tsx`** with React Query provider + React Router
- **Build verified**: `✓ built in 976ms`

#### Git
- **Repository**: [https://github.com/Sach21605in20/Suraksha-Setu.git](https://github.com/Sach21605in20/Suraksha-Setu.git)
- **Branch**: `main`
- **History**:
  - Initialized backend project structure (Spring Boot 3.2)
  - Cleaned up misplaced frontend files
  - Removed documentation and text files from version control (as requested)
- **Latest Commit**: `Initialising Development of SurakshaSetu...`

---

## Phase 2: Authentication System (Backend) ✅
**Status**: Backend Security Implemented.

### What was built
#### Backend Security
- **JWT Implementation**: `JwtUtil` using `jjwt` 0.12.3 with HMAC-SHA256
- **Spring Security**: Stateless session management, CSRF disabled, CORS configured
- **Token Strategy**:
  - Access Token (30 min) -> JSON Response
  - Refresh Token (7 days) -> HttpOnly Cookie
- **Role-Based Access Control**: `ADMIN`, `SURGEON`, `NURSE` roles supported
- **Components**:
  - `AuthController`: Login, Refresh, Logout endpoints
  - `AuthService`: Authentication logic
  - `GlobalExceptionHandler`: Centralized error handling
  - `UserDetailsServiceImpl`: Database integration
- **Verification**:
  - Validated build success with `mvnw clean compile`
  - Verified AuthController endpoints with JUnit 5 + MockMvc (`AuthControllerTest.java`)
  - Seeded Admin User (`admin@orthowatch.com` / `password`)
  - Configured PostgreSQL Timezone to `Asia/Kolkata`

## ⚠️ Known Gaps (Flagged 2026-02-23)

### Rate Limiting — NOT implemented (Phase 2.1 gap)
- **Scope**: `POST /api/v1/auth/login` and `POST /api/v1/auth/refresh`
- **Finding**: No `RateLimitingFilter`, no Bucket4j, no Resilience4j, no Redis-backed throttle exists anywhere in `backend/src`. `SecurityConfig.java` and `AuthController.java` inspected — confirmed clean miss.
- **Risk**: Login endpoint is open to brute-force attacks. Any IP can hammer credentials indefinitely.
- **Resolution plan**: Implement Bucket4j + Redis rate limiter (5 attempts / 15 min per IP) in **Phase 3** (Core Backend) before production deployment. Backend already has Redis in stack (`spring-boot-starter-data-redis` in `pom.xml`).

---

## Next: Phase 2.2 — Authentication System (Frontend)
- Frontend: Login page, auth store (Zustand, memory-only), silent refresh on bootstrap, protected routes
