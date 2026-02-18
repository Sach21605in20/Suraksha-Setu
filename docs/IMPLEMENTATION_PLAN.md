# Implementation Plan & Build Sequence

## OrthoWatch – Post-Discharge Monitoring System

**Project**: OrthoWatch Pilot
**MVP Target Date**: End of Month 2 (internal testing ready)
**Pilot Launch**: Month 3
**Team**: Solo developer (learning Java + Maven)
**Budget**: $0
**Approach**: Iterative development with continuous testing

### Build Philosophy

- Code follows documentation (PRD → APP_FLOW → TECH_STACK → BACKEND_STRUCTURE)
- Test after every step — nothing proceeds without green checks
- TDD for business logic (risk engine, scoring) per `docs/workflow-guides/tdd.md`
- Verify per `docs/workflow-guides/verification-patterns.md` adapted to Spring Boot stack
- Deploy to staging after each milestone
- Gather feedback before continuing

### Phase Dependency Map

```
Phase 1: Foundation ──► Phase 2: Auth ──► Phase 3: Core Backend ──┐
                                                                    ├──► Phase 5: Dashboard
Phase 4: WhatsApp ────────────────────────────────────────────────┘     Frontend
    │
    └──► Phase 6: Integration Testing ──► Phase 7: Deployment ──► Phase 8: Pilot Launch
```

---

## Phase 1: Project Setup & Foundation

### Step 1.1: Initialize Backend Project

**Duration**: 2 hours
**Goal**: Spring Boot project compiles and runs locally

**Tasks**:

1. Create project via Spring Initializr (https://start.spring.io)
   - Group: `com.orthowatch`
   - Artifact: `backend`
   - Java 21, Spring Boot 3.2.2, Maven, JAR packaging
   - Starters: Web, Data JPA, Security, Validation, Actuator, Quartz, Data Redis
2. Extract into `backend/` directory
3. Add remaining dependencies to `pom.xml` per TECH_STACK.md §8
   - PostgreSQL driver, Flyway, jjwt, HAPI FHIR, Lombok, MapStruct, SpringDoc OpenAPI, Sentry, DevTools, Testcontainers, REST Assured
4. Configure Maven Wrapper (`mvnw`, `mvnw.cmd`)
5. Setup Spotless plugin for Google Java Style
6. Verify build:
   ```bash
   ./mvnw clean compile
   ./mvnw spotless:apply
   ```
7. Initialize Git repo, create `.gitignore`, initial commit

**Success Criteria**:
- [ ] `./mvnw clean compile` succeeds with zero errors
- [ ] `./mvnw spotless:check` passes
- [ ] `.gitignore` excludes `target/`, `.env`, `*.jar`, IDE files
- [ ] Maven Wrapper works without global Maven install

**Reference Docs**: TECH_STACK.md §3, §5, §7, §8

---

### Step 1.2: Environment & Configuration

**Duration**: 30 minutes
**Goal**: Application profiles configured for dev and prod

**Tasks**:

1. Create `application.yml` with shared config
2. Create `application-dev.yml` — local Docker DB/Redis, debug logging
3. Create `application-prod.yml` — Neon DB, Upstash Redis, info logging
4. Create `.env.example` with all variables from TECH_STACK.md §6
5. Add `.env` to `.gitignore`

**Success Criteria**:
- [ ] App starts with `dev` profile: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- [ ] All env vars documented in `.env.example`
- [ ] No secrets in version control

**Reference Docs**: TECH_STACK.md §6

---

### Step 1.3: Local Infrastructure (Docker)

**Duration**: 1 hour
**Goal**: PostgreSQL 16 and Redis 7 running locally via Docker

**Tasks**:

1. Create `docker-compose.yml`:
   ```yaml
   services:
     postgres:
       image: postgres:16
       ports: ["5432:5432"]
       environment:
         POSTGRES_DB: orthowatch
         POSTGRES_USER: orthowatch_user
         POSTGRES_PASSWORD: orthowatch_pass
       volumes: [postgres_data:/var/lib/postgresql/data]
     redis:
       image: redis:7-alpine
       ports: ["6379:6379"]
   volumes:
     postgres_data:
   ```
2. Start services: `docker-compose up -d`
3. Verify PostgreSQL connection via `psql` or IDE
4. Verify Redis connection via `redis-cli ping`

**Success Criteria**:
- [ ] `docker-compose up -d` starts both services
- [ ] PostgreSQL accepts connections on port 5432
- [ ] Redis responds to PING on port 6379
- [ ] App connects to both (check Spring Boot startup logs)

**Reference Docs**: TECH_STACK.md §3 (Database, Caching)

---

### Step 1.4: Database Schema & Migrations

**Duration**: 3 hours
**Goal**: All 13 tables created via Flyway migrations

**Tasks**:

1. Create `src/main/resources/db/migration/V1__initial_schema.sql`
   - All 13 tables from BACKEND_STRUCTURE.md §3:
   - `users`, `patients`, `recovery_templates`, `episodes`, `daily_responses`, `risk_scores`, `alerts`, `wound_images`, `consent_logs`, `clinical_audit_log`, `risk_rules`, `sessions`, `hospital_settings`
   - All indexes, constraints, CHECK constraints, foreign keys
2. Create `V2__seed_templates.sql`
   - 3 default templates: TKR, THR, ACL with checklist_config and milestone_config JSONB
   - Default risk rules: FEVER_HIGH, DVT_SYMPTOMS, PAIN_SPIKE, SWELLING_TREND, WOUND_REDNESS
3. Create `V3__seed_admin_user.sql`
   - Default admin account (password: must-change-on-first-login, BCrypt-hashed)
4. Run migrations:
   ```bash
   ./mvnw spring-boot:run  # Flyway auto-migrates on startup
   ```
5. Verify tables via `psql` or IDE database tool

**Success Criteria**:
- [ ] Flyway runs all 3 migrations on startup — no errors
- [ ] All 13 tables exist with correct columns and constraints
- [ ] 3 recovery templates seeded (TKR, THR, ACL)
- [ ] 5 default risk rules seeded
- [ ] Admin user exists in `users` table
- [ ] Foreign key relationships verified (e.g., episodes → patients)

**Reference Docs**: BACKEND_STRUCTURE.md §2, §3 (all tables)

---

### Step 1.5: JPA Entity Classes

**Duration**: 4 hours
**Goal**: All 13 JPA entity classes mapped to database tables

**Tasks**:

1. Create entity classes in `com.orthowatch.model/`:
   - `User.java`, `Patient.java`, `RecoveryTemplate.java`, `Episode.java`
   - `DailyResponse.java`, `RiskScore.java`, `Alert.java`, `WoundImage.java`
   - `ConsentLog.java`, `ClinicalAuditLog.java`, `RiskRule.java`
   - `Session.java`, `HospitalSettings.java`
2. Use Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
3. Configure JPA auditing: `@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`
4. Create Spring Data JPA repositories for each entity
5. Write a simple integration test verifying entity persistence

**Success Criteria**:
- [ ] App starts without Hibernate validation errors
- [ ] Each entity maps correctly to its table (no column mismatches)
- [ ] `@CreatedDate` and `@LastModifiedDate` populate automatically
- [ ] Repository `save()` + `findById()` works for each entity
- [ ] `./mvnw test` passes

**Reference Docs**: BACKEND_STRUCTURE.md §3, TECH_STACK.md §3 (ORM)

---

### Step 1.6: Initialize Frontend Project

**Duration**: 1 hour
**Goal**: React + Vite + TypeScript project running locally

**Tasks**:

1. Create frontend:
   ```bash
   npx -y create-vite@latest frontend -- --template react-ts
   cd frontend && npm install
   ```
2. Install dependencies per TECH_STACK.md §8 (Frontend):
   ```bash
   npm install axios @tanstack/react-query zustand react-hook-form zod @hookform/resolvers recharts lucide-react react-router-dom date-fns clsx tailwind-merge
   npm install -D tailwindcss postcss autoprefixer eslint prettier @playwright/test
   npx tailwindcss init -p
   ```
3. Setup shadcn/ui: `npx shadcn-ui@latest init`
4. Configure Tailwind with medical color palette
5. Verify: `npm run dev` → opens on localhost:5173

**Success Criteria**:
- [ ] `npm run dev` starts Vite dev server
- [ ] TypeScript compiles without errors
- [ ] Tailwind styles apply correctly
- [ ] shadcn/ui components available
- [ ] ESLint + Prettier configured

**Reference Docs**: TECH_STACK.md §2, §8 (Frontend Dependencies)

---

## Phase 2: Authentication System

### Step 2.1: Backend — JWT + Spring Security

**Duration**: 4 hours
**Goal**: Login/logout/refresh endpoints working with JWT

**Tasks**:

1. Create `JwtUtil.java` — token generation, validation, extraction
   - Access token: 30 min, contains sub, email, role, fullName
   - Refresh token: 7 days, contains sub, sessionId
   - Signing: HMAC-SHA256 with `JWT_SECRET`
2. Create `SecurityConfig.java`:
   - JWT filter chain (extract from cookie or Authorization header)
   - CORS: allow frontend origin only
   - CSRF: disabled (stateless JWT)
   - Session: STATELESS
   - Public routes: `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/webhook/**`, `/actuator/health`
   - Role annotations: `@EnableMethodSecurity`
3. Create `AuthController.java` + `AuthService.java`:
   - `POST /api/v1/auth/login` — validate credentials, issue tokens, set cookies
   - `POST /api/v1/auth/refresh` — validate refresh token, issue new access token
   - `POST /api/v1/auth/logout` — invalidate session, clear cookies
4. Create DTOs: `LoginRequest`, `LoginResponse`, `ErrorResponse`
5. Create `GlobalExceptionHandler.java` with standard error format
6. Test with REST client (Swagger UI at `/swagger-ui.html`)

**Success Criteria**:
- [ ] Login returns JWT tokens in HTTP-only cookies
- [ ] Access token expires after 30 minutes
- [ ] Refresh endpoint issues new access token
- [ ] Logout clears cookies and invalidates session
- [ ] Invalid credentials return 401
- [ ] Rate limiting: 5 login attempts per 15 min per IP
- [ ] Swagger UI accessible at `/swagger-ui.html`
- [ ] `./mvnw test` — auth tests pass

**Reference Docs**: BACKEND_STRUCTURE.md §4 (Auth Endpoints), §5 (Auth & Authorization), TECH_STACK.md §3 (Authentication)

---

### Step 2.2: Frontend — Login Page

**Duration**: 3 hours
**Goal**: Clinician login page connected to backend

**Tasks**:

1. Create route structure with React Router: `/login`, `/dashboard`, `/patients/:id`
2. Build `LoginPage.tsx`:
   - Email input with validation
   - Password input
   - "Sign In" button with loading state
   - Error message display (inline)
3. Create `AuthContext` / Zustand auth store — track user, role, token state
4. Create Axios instance with interceptors:
   - Auto-include credentials (cookies)
   - 401 response → redirect to login
   - Error normalization
5. Create `ProtectedRoute` component — redirects unauthenticated users to `/login`
6. Test login flow end-to-end

**Success Criteria**:
- [ ] Login form submits to backend, receives JWT
- [ ] Successful login redirects to `/dashboard`
- [ ] Invalid credentials show inline error
- [ ] Loading spinner shows during API call
- [ ] Protected routes redirect to login when unauthenticated
- [ ] Session persists across page refresh (cookie-based)

**Reference Docs**: APP_FLOW.md §2 (Flow 4 — Login), §4 (Screen: Login), BACKEND_STRUCTURE.md §4 (Auth Endpoints)

---

## Phase 3: Core Backend — Enrollment, Checklist, Risk Engine

### Step 3.1: Enrollment Service

**Duration**: 4 hours
**Goal**: Patients can be enrolled into monitoring system

**Tasks**:

1. Create `EnrollmentController.java` — `POST /api/v1/enrollments`
2. Create `EnrollmentService.java`:
   - Validate request (phone format, surgeon exists, template exists, baseline fields)
   - Create or reuse `Patient` record (match by phone)
   - Create `Episode` record with baseline snapshot
   - Create `ConsentLog` entry (PENDING)
   - Schedule consent timeout job (24h) via Quartz
3. Create `EnrollmentRequest` DTO with Bean Validation annotations
4. Create MapStruct mappers: `PatientMapper`, `EpisodeMapper`
5. Write unit tests for validation logic
6. Write integration test for end-to-end enrollment

**Success Criteria**:
- [ ] `POST /api/v1/enrollments` creates patient + episode + consent log
- [ ] Duplicate phone reuses existing patient, creates new episode
- [ ] Missing baseline fields return 400 with field-level errors
- [ ] Invalid surgeon ID returns 404
- [ ] Unknown surgery type returns 404
- [ ] Consent timeout job scheduled (verify in Quartz tables)
- [ ] `./mvnw test` passes

**Reference Docs**: APP_FLOW.md §2 (Flow 1), BACKEND_STRUCTURE.md §4 (Enrollment Endpoints)

---

### Step 3.2: Risk Engine (TDD)

**Duration**: 6 hours
**Goal**: Rule-based risk scoring engine calculates composite scores

> ⚠️ **TDD Approach**: Follow `docs/workflow-guides/tdd.md` — RED → GREEN → REFACTOR for all risk engine logic.

**Tasks**:

1. **Write tests FIRST** for `RiskEngineService`:
   - Test: fever >100°F → HIGH risk, weight 30
   - Test: pain spike >2 day-over-day → MEDIUM, weight 15
   - Test: increasing swelling trend over 2 days → MEDIUM, weight 15
   - Test: DVT symptoms present → HIGH, weight 30
   - Test: composite score calculation (0–100)
   - Test: risk classification (0–30 LOW, 31–60 MEDIUM, 61–100 HIGH)
   - Test: trajectory calculation (IMPROVING, STABLE, WORSENING)
   - Test: rule version snapshot stored with each calculation
   - Test: Day 1 patient (no history) scores absolute values only
2. Create `RiskRuleEvaluator.java` — load rules from DB, evaluate conditions
3. Create `RiskEngineService.java` — process daily response, calculate composite score
4. Load default rules from `risk_rules` table (seeded in V2 migration)
5. Store results in `risk_scores` table with `rule_version_id` + `rule_set_snapshot`
6. Generate `Alert` records for HIGH risk scores

**Success Criteria**:
- [ ] All TDD tests pass (RED → GREEN → REFACTOR complete)
- [ ] Composite score calculated correctly (0–100)
- [ ] Risk levels match thresholds: LOW ≤30, MEDIUM 31–60, HIGH 61–100
- [ ] HIGH risk creates alert in `alerts` table
- [ ] Trajectory computed from 3-day trend
- [ ] Rule version snapshot stored in JSONB
- [ ] `./mvnw test` — all risk engine tests green

**Reference Docs**: APP_FLOW.md §2 (Flow 3), BACKEND_STRUCTURE.md §3 (risk_scores, alerts, risk_rules)

---

### Step 3.3: Checklist Service & Scheduled Tasks

**Duration**: 3 hours
**Goal**: Automated daily checklist scheduling and response processing

> 💡 **Complexity Cut**: Using Spring `@Scheduled` instead of Quartz for MVP. Pilot = 1 hospital, 1 timezone (IST). Quartz with JDBC job store deferred to post-pilot when multi-hospital/multi-timezone support is needed.

**Tasks**:

1. Create `ChecklistService.java`:
   - Process incoming checklist responses (map WhatsApp payload → `DailyResponse`)
   - Determine completion_status: PENDING, PARTIAL, COMPLETED
   - Apply hard stop rule: all required fields + mandatory image = COMPLETED
   - Trigger risk engine after each complete response
2. Create `ScheduledTasks.java` with `@Scheduled` methods:
   - `dispatchDailyChecklists()` — `@Scheduled(cron = "0 0 9 * * *")` (9 AM IST)
   - `sendReminders()` — `@Scheduled(fixedRate = 60000)` polls for 4-hour non-responses
   - `escalateNonResponses()` — `@Scheduled(fixedRate = 60000)` polls for 8-hour non-responses, creates NON_RESPONSE alert
   - `checkConsentTimeouts()` — `@Scheduled(fixedRate = 300000)` polls for 24-hour consent timeouts
   - `cleanupExpiredSessions()` — `@Scheduled(cron = "0 0 2 * * *")` daily at 2 AM
3. Store scheduled task state in DB (query `episodes` + `daily_responses` timestamps)
4. Write tests for each scheduled scenario

**Success Criteria**:
- [ ] Daily checklists dispatched at 9 AM IST for all active episodes
- [ ] Reminders sent 4 hours after dispatch if no response
- [ ] Escalation alert created 8 hours after dispatch for non-responses
- [ ] Late response after escalation: cancels alert if unreviewed, recalculates risk
- [ ] Consent timeout alerts nurse if not received in 24h
- [ ] `./mvnw test` passes

**Reference Docs**: APP_FLOW.md §2 (Flow 2 — Error States, Edge Cases)

---

### Step 3.4: Alert & Escalation Service

**Duration**: 3 hours
**Goal**: Alert lifecycle management with SLA tracking

**Tasks**:

1. Create `AlertService.java`:
   - Create alerts (HIGH_RISK, NON_RESPONSE, EMERGENCY_OVERRIDE, CONSENT_TIMEOUT)
   - Acknowledge alerts
   - Resolve alerts with escalation outcome (required selection)
   - Auto-forward to secondary clinician if SLA exceeded
2. Create `AlertController.java`:
   - `POST /api/v1/alerts/{alertId}/acknowledge`
   - `POST /api/v1/alerts/{alertId}/resolve` (with outcome + notes)
3. Create `AuditService.java`:
   - Log every clinician action to `clinical_audit_log`
   - Immutable entries (no UPDATE/DELETE)
4. Write tests for alert lifecycle: PENDING → ACKNOWLEDGED → RESOLVED
5. Write tests for SLA auto-forwarding

**Success Criteria**:
- [ ] Alerts created automatically by risk engine and scheduled tasks
- [ ] Acknowledge endpoint updates status + timestamp
- [ ] Resolve requires escalation outcome selection
- [ ] SLA auto-forward works (alert re-assigned to secondary after threshold)
- [ ] Every action creates audit log entry
- [ ] Audit log entries are immutable
- [ ] `./mvnw test` passes

**Reference Docs**: APP_FLOW.md §2 (Flow 4), BACKEND_STRUCTURE.md §3 (alerts, clinical_audit_log), §4 (Alert Endpoints)

---

## Phase 4: WhatsApp Integration & Storage

### Step 4.0: Wound Image Storage Setup

**Duration**: 1.5 hours
**Goal**: Image storage working for local dev and pilot deployment

> 💡 **Complexity Cut**: Two strategies only — local filesystem for dev, Supabase Storage for pilot. Cloudflare R2 abstraction deferred to post-pilot.

**Tasks**:

1. Create `ImageStorageService.java` with profile-based implementation:
   - `LocalStorageStrategy` — saves to `./uploads/wound-images/` (dev)
   - `SupabaseStorageStrategy` — uploads to Supabase Storage bucket (pilot)
2. Profile-based selection:
   - `application-dev.yml`: `storage.provider=LOCAL`, `storage.local-path=./uploads/`
   - `application-prod.yml`: `storage.provider=SUPABASE`, `storage.bucket=wound-images`
3. Create Supabase Storage bucket:
   - Bucket name: `wound-images`
   - Access: private (authenticated download only)
   - Max file size: 10 MB
   - Allowed types: `image/jpeg`, `image/png`
4. Create `ImageController.java`:
   - `GET /api/v1/images/{imageId}` — serves image with audit logging
   - Validates user has SURGEON or NURSE role
5. Add `.uploads/` to `.gitignore`
6. Write unit tests for each storage strategy

**Success Criteria**:
- [ ] Local dev: image saved to filesystem, retrievable via API
- [ ] Supabase: image uploaded to bucket, downloadable via signed URL
- [ ] 10 MB limit enforced (413 returned for oversized files)
- [ ] Only `image/jpeg` and `image/png` accepted (415 for others)
- [ ] `GET /api/v1/images/{imageId}` creates `VIEW_IMAGE` audit log entry
- [ ] `./mvnw test` passes

**Reference Docs**: BACKEND_STRUCTURE.md §3 (wound_images table), TECH_STACK.md §3 (File Storage)

---

### Step 4.1: WhatsApp Business API Setup

**Duration**: 2 hours (setup) + variable (Meta approval)
**Goal**: WhatsApp test sandbox connected and sending messages

**Tasks**:

1. Create Meta Developer account → Meta Business Suite
2. Create WhatsApp Business App → get Phone Number ID + Access Token
3. Configure webhook URL: `https://<ngrok-or-render>/api/v1/webhook/whatsapp`
4. Set verify token in env: `WHATSAPP_VERIFY_TOKEN`
5. Submit message templates for approval:
   - `consent_request` — consent capture message
   - `welcome_message` — post-consent welcome
   - `daily_checklist` — interactive button checklist
   - `reminder_4h` — non-response reminder
   - `emergency_override` — breathlessness alert
   - `emergency_followup` — "Are you going to the hospital?"
6. Test with sandbox phone number

**Success Criteria**:
- [ ] WhatsApp webhook verified (GET endpoint echoes challenge)
- [ ] Can send test message from API to sandbox number
- [ ] Webhook receives inbound messages (POST endpoint logs payload)
- [ ] At least `daily_checklist` template approved by Meta
- [ ] Access token stored securely in environment variables

**Reference Docs**: TECH_STACK.md §3 (WhatsApp), APP_FLOW.md §2 (Flow 2)

> ⚠️ **Timeline Risk**: Meta template approval can take 1–7 days. Start this step early and parallelize with other work.

---

### Step 4.2: WhatsApp Service — Outbound Messages

**Duration**: 4 hours
**Goal**: System sends structured checklist messages via WhatsApp

**Tasks**:

1. Create `WhatsAppService.java`:
   - Send consent request message
   - Send welcome message (in patient's language)
   - Send daily checklist (interactive buttons: pain, swelling, fever, DVT, mobility, medication)
   - Send wound image request (Day 3 & 5)
   - Send reminder (4-hour)
   - Send emergency override message
   - Send emergency follow-up ("Are you going to hospital?")
2. Create `WhatsAppApiClient.java` — HTTP client for Meta Graph API
3. Handle template variables (patient name, day number, surgery type)
4. Handle language variants (en, hi, ta)
5. Write unit tests with mocked API responses

**Success Criteria**:
- [ ] Consent message sends successfully
- [ ] Daily checklist sends with interactive buttons
- [ ] Wound image prompt sends on Day 3 and 5
- [ ] Emergency override sends immediately on breathlessness
- [ ] Messages use patient's preferred language
- [ ] API errors logged to Sentry, retried 3 times
- [ ] `./mvnw test` passes

**Reference Docs**: APP_FLOW.md §2 (Flow 1 steps 5–6, Flow 2)

---

### Step 4.3: WhatsApp Webhook — Inbound Processing

**Duration**: 4 hours
**Goal**: System receives and processes patient responses

**Tasks**:

1. Create `WebhookController.java`:
   - `GET /api/v1/webhook/whatsapp` — verification endpoint
   - `POST /api/v1/webhook/whatsapp` — inbound message handler
2. Parse Meta webhook payload:
   - Map phone number → patient → active episode
   - Parse button_reply → identify question type + answer
   - Parse image attachment → wound image upload
3. Route to `ChecklistService`:
   - Update corresponding field in `daily_responses`
   - Track completion_status progression
   - On all required fields complete → trigger risk engine
4. Handle emergency override:
   - If DVT response includes "Breathlessness" → immediate emergency message
   - Skip remaining questions
   - Create CRITICAL alert
   - Schedule follow-up confirmation (5 min)
5. Handle consent responses (YES → update episode, trigger welcome)
6. Handle free-text → respond "Please use buttons to respond"
7. Always return 200 to Meta (even on errors)
8. Write integration tests covering all response types

**Success Criteria**:
- [ ] Webhook verification works (Meta can subscribe)
- [ ] Button replies parsed and stored correctly for all 7 checklist fields
- [ ] Image uploads saved to storage and linked to episode
- [ ] Emergency override triggers immediate message + CRITICAL alert
- [ ] Consent response updates episode status
- [ ] Caregiver responses tracked with `responder_type = CAREGIVER`
- [ ] 200 returned to Meta for all payloads
- [ ] `./mvnw test` passes

**Reference Docs**: APP_FLOW.md §2 (Flow 2), BACKEND_STRUCTURE.md §4 (Webhook Endpoints)

---

## Phase 5: Clinician Dashboard (Frontend)

### Step 5.1: Dashboard — Top Risk & Non-Responsive

**Duration**: 6 hours
**Goal**: Main dashboard showing Top 10 risk patients and non-responsive list

**Tasks**:

1. Build `DashboardPage.tsx`:
   - Top 10 patients sorted by risk score (descending)
   - Risk level color coding: LOW (green), MEDIUM (amber), HIGH (red)
   - Sparkline trend charts (5-day risk trajectory via Recharts)
   - "New Today" badge for first-time HIGH patients
   - "Non-Responsive Today" section with hours-since-last-response
2. Create API hooks (`useDashboard`) via TanStack Query:
   - `GET /api/v1/dashboard/top-risk` — auto-refresh every 5 minutes
   - Stale-while-revalidate strategy
3. Implement "Mark Reviewed" action button → `POST /api/v1/patients/{episodeId}/mark-reviewed`
4. Add loading skeletons and error states
5. Responsive layout: 1-column mobile, 2-column tablet, 3-column desktop

**Success Criteria**:
- [ ] Dashboard loads within 2 seconds
- [ ] Risk scores display with correct color coding
- [ ] Sparkline charts render 5-day trends
- [ ] "New Today" badge appears for new HIGH patients
- [ ] Non-responsive patients listed with elapsed hours
- [ ] "Mark Reviewed" removes patient from list
- [ ] Auto-refresh fires every 5 minutes
- [ ] Loading skeletons show during fetch

**Reference Docs**: APP_FLOW.md §4 (Dashboard screen), BACKEND_STRUCTURE.md §4 (Dashboard Endpoints)

---

### Step 5.2: Patient Detail View

**Duration**: 5 hours
**Goal**: Full patient episode detail with all clinical data

**Tasks**:

1. Build `PatientDetailPage.tsx`:
   - Header: patient name, surgery type, day post-op, risk level badge
   - Recovery timeline: day-by-day responses in expandable accordion
   - Risk score chart (line chart, 14-day x-axis)
   - Wound images gallery (Day 3, 5 thumbnails, click-to-zoom)
   - Active alerts section with Acknowledge/Resolve buttons
   - Clinical audit log (read-only, scrollable)
2. Create API hooks (`usePatientDetail`) → `GET /api/v1/patients/{episodeId}`
3. Build alert resolution modal:
   - Escalation outcome dropdown: OPD_SCHEDULED, TELEPHONIC_ADVICE, MEDICATION_ADJUSTED, ER_REFERRAL, FALSE_POSITIVE
   - Notes textarea (max 2000 characters)
   - "Resolve" button with confirmation
4. Build wound image viewer (click thumbnail → full-size modal, audit logged)
5. Baseline comparison: show discharge values alongside current day

**Success Criteria**:
- [ ] All episode data renders correctly
- [ ] Recovery timeline shows complete/partial/pending per day
- [ ] Risk chart shows 14-day trend line
- [ ] Wound images load and display in modal
- [ ] Alert acknowledge/resolve works end-to-end
- [ ] Escalation outcome selection is required
- [ ] Audit log entries appear after each action
- [ ] Baseline discharge values shown for comparison

**Reference Docs**: APP_FLOW.md §4 (Patient Detail screen), §3 (Interaction Patterns)

---

### Step 5.3: Patient List & Enrollment Form

**Duration**: 4 hours
**Goal**: Filterable patient list and enrollment UI

**Tasks**:

1. Build `AllPatientsPage.tsx`:
   - Table view with columns: Name, Surgery, Day, Risk, Status, Trend
   - Filters: surgery type, risk level, response status
   - Pagination (20 per page)
   - Sort by risk score, day, name
2. Build `EnrollmentForm.tsx` (for NURSE/ADMIN roles):
   - Patient demographics (name, age, phone, caregiver phone, language)
   - Surgery details (type, date, discharge date, surgeon selection)
   - Baseline capture (discharge pain score slider, swelling dropdown)
   - Form validation via Zod + react-hook-form
   - Submit to `POST /api/v1/enrollments`
3. Role-based UI: hide enrollment button for SURGEON role
4. Success flow: show "Patient enrolled — consent message sent" toast

**Success Criteria**:
- [ ] Patient list renders with all columns
- [ ] Filters work independently and in combination
- [ ] Pagination navigates correctly
- [ ] Enrollment form validates all required fields
- [ ] Baseline fields (pain, swelling) are mandatory
- [ ] Successful enrollment shows confirmation toast
- [ ] Form resets after successful submission
- [ ] SURGEON role cannot see enrollment button

**Reference Docs**: APP_FLOW.md §2 (Flow 1), §4 (All Patients screen), BACKEND_STRUCTURE.md §4 (Patient + Enrollment Endpoints)

---

### Step 5.4: Admin Screens — DEFERRED TO POST-PILOT

> 💡 **Complexity Cut**: Admin UI for editing templates and risk rules is deferred. For pilot, templates and rules are seeded via Flyway migrations (V2) and modified via direct API calls or database if needed. Backend admin APIs (Step 3.4) remain available.
>
> **Post-pilot**: Build `AdminTemplatesPage.tsx` (JSON editor, versioning) and `AdminRulesPage.tsx` (rule weights, active toggle, version bump UI).

**What remains for MVP**:
- Backend `GET/PUT /api/v1/admin/templates/*` and `GET/PUT /api/v1/admin/rules/*` APIs exist
- `@PreAuthorize("hasRole('ADMIN')")` enforced on all admin endpoints
- Templates and rules manageable via Swagger UI or cURL during pilot

**Reference Docs**: BACKEND_STRUCTURE.md §4 (Admin Endpoints)

---

## Phase 6: Testing & Refinement

### Step 6.1: Unit & Integration Tests (Backend)

**Duration**: 6 hours
**Goal**: Critical paths covered with automated tests

**Tasks**:

1. Unit tests (JUnit 5 + Mockito):
   - `RiskEngineServiceTest` — all scoring scenarios (already done via TDD in 3.2)
   - `AuthServiceTest` — login, refresh, logout
   - `EnrollmentServiceTest` — validation, deduplication
   - `AlertServiceTest` — lifecycle, SLA
   - `WhatsAppServiceTest` — message formatting, error handling
   - `ChecklistServiceTest` — completion logic, hard stops
2. Integration tests (Testcontainers + PostgreSQL):
   - Full enrollment flow: enroll → consent → first checklist → response → risk score
   - Alert flow: high risk → alert created → acknowledge → resolve
   - Auth flow: login → access protected resource → token refresh → logout
3. REST API tests (REST Assured):
   - All endpoints return correct status codes
   - Validation errors return standardized error format
   - Role-based access enforced (SURGEON cannot access admin endpoints)

**Test Coverage Targets**:
- Risk Engine: 95%
- Authentication: 90%
- Enrollment: 85%
- Alert/Escalation: 85%
- Overall: ≥80%

**Success Criteria**:
- [ ] `./mvnw test` — all tests pass
- [ ] Coverage report generated (JaCoCo): ≥80% overall
- [ ] Risk engine: 95% line coverage
- [ ] Zero flaky tests
- [ ] Testcontainers used for DB-dependent tests (no H2)
- [ ] Tests run in CI (< 5 minutes)

**Reference Docs**: TECH_STACK.md §4 (Testing), `docs/workflow-guides/tdd.md`, `docs/workflow-guides/verification-patterns.md`

---

### Step 6.2: Frontend Tests & E2E

**Duration**: 2 hours
**Goal**: Critical user flows verified end-to-end

> 💡 **Complexity Cut**: E2E tests cover only the 3 most critical flows. Backend integration tests (Step 6.1) carry the testing weight. Visual regression and accessibility audits deferred to post-pilot.

**Tasks**:

1. Playwright E2E tests (3 critical flows only):
   - Login flow: valid credentials → dashboard loads
   - Dashboard: top-risk patients display, mark reviewed works
   - Alert resolution: navigate to patient, resolve alert with outcome
2. Component tests (Vitest):
   - LoginForm validation states
   - DashboardCard risk color coding

**Success Criteria**:
- [ ] All 3 Playwright E2E tests pass
- [ ] Component tests pass
- [ ] Tests run in CI via GitHub Actions

**Reference Docs**: TECH_STACK.md §4 (Testing), APP_FLOW.md (Flows 4, 1)

---

## Phase 7: Deployment & Infrastructure

### Step 7.1: CI/CD Pipeline (GitHub Actions)

**Duration**: 3 hours
**Goal**: Automated build, test, and deploy pipeline

**Tasks**:

1. Create `.github/workflows/backend-ci.yml`:
   - Trigger: push to `main`, PR to `main`
   - Steps: checkout → setup Java 21 → cache Maven → `./mvnw verify` → Testcontainers tests
   - Fail on: test failure, Spotless violation, SpotBugs findings
2. Create `.github/workflows/frontend-ci.yml`:
   - Trigger: push to `main`, PR to `main`
   - Steps: checkout → setup Node 20 → `npm ci` → lint → type-check → test → build
3. Create `.github/workflows/deploy.yml`:
   - Trigger: push to `main` (after CI passes)
   - Backend: build JAR → deploy to Render
   - Frontend: deploy to Vercel (Vercel auto-deploy on push)
4. Add status badges to `README.md`

**Success Criteria**:
- [ ] Backend CI runs on every PR — blocks merge on failure
- [ ] Frontend CI runs on every PR — blocks merge on failure
- [ ] Deploy workflow triggers on main branch push
- [ ] Backend deploys to Render successfully
- [ ] Frontend deploys to Vercel successfully
- [ ] Pipeline completes in < 10 minutes

**Reference Docs**: TECH_STACK.md §4 (CI/CD), §7 (Maven Commands)

---

### Step 7.2: Deploy to Pilot Infrastructure

**Duration**: 4 hours
**Goal**: App running on free-tier cloud services

**Tasks**:

1. **Neon (Database)**:
   - Create Neon project → get connection string
   - Run Flyway migrations on first deploy
   - Enable point-in-time recovery
2. **Upstash (Redis)**:
   - Create Upstash Redis instance → get URL + token
   - Configure TLS connection in `application-prod.yml`
3. **Supabase Storage**:
   - Create bucket for wound images (already configured in Step 4.0)
   - Set production API keys in environment
4. **Render (Backend)**:
   - Create Web Service → connect GitHub repo
   - Set environment variables
   - Configure health check: `/actuator/health`
   - Set instance type: free tier (512 MB RAM)
5. **Vercel (Frontend)**:
   - Connect GitHub repo → auto-deploy on push
   - Set `VITE_API_URL` to Render backend URL
6. **Monitoring**:
   - Sentry DSN configured in both frontend and backend
   - UptimeRobot: monitor `/actuator/health` every 5 min
   - Spring Actuator: expose health, info, metrics endpoints

> 💡 **Complexity Cut**: Using default Render/Vercel domains for pilot (`*.onrender.com`, `*.vercel.app`). Custom domain (`orthowatch.in`) + Cloudflare DNS deferred to post-pilot. Saves DNS propagation + SSL debugging.

**Success Criteria**:
- [ ] API accessible at Render default URL `/actuator/health`
- [ ] Frontend accessible at Vercel default URL
- [ ] Database migrations ran successfully on Neon
- [ ] Redis connection works (sessions, caching)
- [ ] Image upload/download works via Supabase Storage
- [ ] Sentry captures test error
- [ ] UptimeRobot shows 100% uptime after 24h

**Reference Docs**: TECH_STACK.md §3 (Infrastructure), §5 (Deployment)

---

## Phase 8: Pilot Launch & Monitoring

### Step 8.1: Pre-Launch Checklist

**Duration**: 2 hours
**Goal**: All safety and compliance checks completed

**Tasks**:

1. Security audit:
   - Verify no secrets in Git history
   - Verify HTTP-only cookies, Secure flag, SameSite
   - Verify CORS allows only frontend origin
   - Verify rate limiting active on login + API endpoints
   - Verify BCrypt password hashing (12 rounds)
2. Data compliance:
   - Consent capture flow working (DPDP Act 2023)
   - Audit log immutability verified
   - Image encryption at rest confirmed
   - Retention policies documented and enforced
3. Smoke tests on production:
   - Login → dashboard → patient detail → resolve alert
   - WhatsApp: send checklist → respond → verify risk score
   - Enrollment → consent → first response
4. Create default clinician accounts:
   - 2 surgeons, 2 nurses, 1 admin (for pilot hospital)
   - Force password change on first login

**Success Criteria**:
- [ ] Zero security findings in manual audit
- [ ] Consent flow captures DPDP-compliant consent
- [ ] Audit trail captures all clinician actions
- [ ] All smoke tests pass on production
- [ ] Pilot accounts created (5 total)
- [ ] Hospital IT team has admin credentials

---

### Step 8.2: Pilot Operation (3 Months)

**Duration**: 3 months
**Goal**: Monitor 30+ patients, hit PRD success metrics

**Tasks**:

1. Week 1: Onboard hospital staff, training sessions
2. Weeks 1–4: Monitor first 10 patients, fix bugs daily
3. Weeks 5–8: Scale to 30+ patients, optimize performance
4. Weeks 9–12: Collect KPI data, prepare pilot report

**KPI Tracking** (from PRD.md):

| Metric | Target | Measurement |
|---|---|---|
| 30-day readmission rate | <5% (down from 15%) | Compare vs. historical baseline |
| Daily checklist completion | ≥85% | `daily_responses` completion rate |
| Flag-to-intervention time | <2 hours | `alerts.created_at` → `alerts.resolved_at` |
| Patient enrollment | ≥30 patients | `episodes` count where `status != CANCELLED` |
| System uptime | ≥99.5% | UptimeRobot reports |

**Success Criteria**:
- [ ] ≥30 patients enrolled and monitored
- [ ] ≥85% daily checklist completion rate
- [ ] Flag-to-intervention <2 hours (median)
- [ ] Zero data breaches or security incidents
- [ ] Clinician satisfaction survey: ≥4/5 average
- [ ] System uptime ≥99.5%

---

## Milestones & Timeline

### Milestone 1: Foundation Complete

**Target**: End of Week 2
**Deliverables**:
- [ ] Backend compiles + runs (Phase 1 complete)
- [ ] Database schema applied (13 tables)
- [ ] JPA entities mapped
- [ ] Frontend project initialized
- [ ] Docker dev environment working

### Milestone 2: Auth & Core Backend

**Target**: End of Week 4
**Deliverables**:
- [ ] JWT authentication working
- [ ] Login page functional
- [ ] Enrollment service operational
- [ ] Risk engine passing all TDD tests
- [ ] `@Scheduled` tasks running (dispatch, reminders, escalation)
- [ ] Alert lifecycle working

### Milestone 3: WhatsApp Integration

**Target**: End of Week 6
**Deliverables**:
- [ ] WhatsApp sending checklists
- [ ] Inbound responses processed
- [ ] Risk scores calculated from responses
- [ ] Emergency override flow working
- [ ] End-to-end: enroll → consent → checklist → response → risk → alert

### Milestone 4: Dashboard Complete

**Target**: End of Week 7
**Deliverables**:
- [ ] Dashboard showing Top 10 risk patients
- [ ] Patient detail view with history
- [ ] Alert resolution with escalation outcomes
- [ ] Admin APIs functional (UI deferred to post-pilot)
- [ ] Responsive design (mobile + desktop)

### Milestone 5: Tested & Deployed

**Target**: End of Week 8
**Deliverables**:
- [ ] ≥80% test coverage (backend)
- [ ] Playwright E2E tests passing
- [ ] CI/CD pipeline operational
- [ ] Deployed to Render + Vercel + Neon
- [ ] Sentry + UptimeRobot monitoring live

### Milestone 6: Pilot Launch

**Target**: Start of Month 3
**Deliverables**:
- [ ] Pre-launch security audit passed
- [ ] Hospital staff trained
- [ ] First 5 patients enrolled
- [ ] Monitoring dashboards operational

---

## Risk Mitigation

### Technical Risks

| Risk | Impact | Probability | Mitigation |
|---|---|---|---|
| Flyway migration breaks production DB | Critical | Low | Test migrations on Neon branch first; never edit deployed migrations |
| JWT token leak / auth bypass | Critical | Low | HTTP-only cookies, short-lived tokens, refresh rotation, security audit |
| WhatsApp API rate limiting | High | Medium | Queue outbound messages, exponential backoff, Sentry alerts |
| Risk engine false positives | High | Medium | TDD with boundary cases; admin-tunable rules + weights |
| Neon free tier cold starts | Medium | High | Spring Boot startup optimization; UptimeRobot keeps alive |
| Redis connection loss | Medium | Medium | Graceful fallback: skip cache, serve from DB; reconnect logic |
| Large wound images slow upload | Medium | Medium | Client-side compression before upload; max 10 MB limit |

### Timeline Risks

| Risk | Impact | Probability | Mitigation |
|---|---|---|---|
| Scope creep (P1 features requested early) | High | High | Strict P0-only for pilot; defer all P1 to post-pilot backlog |
| Meta WhatsApp template rejection | High | Medium | Submit templates in Week 1; have 2 alternative wordings ready |
| Solo developer burnout | High | Medium | Realistic estimates with 30% buffer; 8-week build, not 4 |
| Hospital IT delays (DNS, network) | Medium | High | Start IT coordination 2 weeks before pilot; have fallback domain |
| Learning curve (Java/Spring Boot) | Medium | High | Follow TECH_STACK.md exactly; use Spring Initializr; leverage IDE |

### Compliance Risks

| Risk | Impact | Probability | Mitigation |
|---|---|---|---|
| DPDP Act 2023 non-compliance | Critical | Low | Digital consent capture with audit trail; privacy-by-design |
| NABH audit trail gaps | High | Low | Immutable `clinical_audit_log`; no DELETE operations |
| Patient data breach | Critical | Low | Encryption at rest, TLS in transit, role-based access, image encryption |

---

## Success Criteria (Overall)

### MVP is successful when:

1. ✅ All P0 features from PRD.md implemented and functional
2. ✅ All 5 user flows from APP_FLOW.md working end-to-end
3. ✅ API matches BACKEND_STRUCTURE.md specification
4. ✅ Tech stack matches TECH_STACK.md (no unapproved substitutions)
5. ✅ ≥80% backend test coverage (JUnit + integration)
6. ✅ Zero critical bugs in production at pilot launch
7. ✅ Performance: dashboard loads <2s, API responses <500ms (p95)
8. ✅ Monthly operating cost: $0 (free tier services only)
9. ✅ WhatsApp checklist→risk→alert pipeline: <30s end-to-end
10. ✅ DPDP consent + NABH audit compliance verified

---

## Post-MVP Roadmap

### After successful pilot, prioritize:

1. **P1 Features** (from PRD.md):
   - Multi-hospital support
   - AI-powered wound image analysis
   - Predictive analytics dashboard
   - PDF report generation for clinical records
2. **User feedback implementation** from pilot clinicians
3. **Performance optimization**:
   - Connection pooling tuning
   - Query optimization for large patient sets
   - CDN for static assets
4. **Analytics integration**:
   - PostHog or Plausible for usage analytics
   - Custom KPI dashboard for hospital administration
5. **P2 Features** (nice-to-haves):
   - Patient mobile app (React Native)
   - EMR deep integration (HL7 FHIR beyond discharge)
   - Multi-language UI (Hindi, Tamil, Telugu)
   - Family member portal
