# Backend Architecture & Database Structure

## OrthoWatch – Post-Discharge Monitoring System

**Last Updated**: Feb 2026
**Version**: 1.0
**Based on**: PRD v1.0 + APP_FLOW v1.2 + TECH_STACK v1.1

---

## 1. Architecture Overview

### System Architecture

- **Pattern**: Modular Monolith — MVC with Service Layer (Controller → Service → Repository)
- **API Style**: RESTful (JSON) — Spring Boot 3.2 on Java 21 LTS
- **Authentication**: JWT-based with HTTP-only cookies (Spring Security 6.2)
- **Authorization**: Role-Based Access Control (RBAC) — ADMIN, SURGEON, NURSE
- **Data Flow**:
  ```
  Client (React SPA / WhatsApp Webhook)
    → Spring Security Filter Chain (JWT validation + RBAC)
      → REST Controller (@RestController)
        → Service Layer (@Service — business logic)
          → Repository Layer (Spring Data JPA)
            → PostgreSQL 16
  
  Async Path:
    Quartz Scheduler → Service Layer → WhatsApp API (outbound)
    WhatsApp Webhook → Controller → Service Layer → DB
  ```
- **Caching Strategy**: Redis 7 (Spring Data Redis) for sessions, dashboard API responses, rate limiting
- **ORM**: Spring Data JPA (Hibernate 6.4) with HikariCP connection pooling
- **Migrations**: Flyway 10.x (SQL-based, versioned)
- **Job Scheduling**: Quartz Scheduler (JDBC-backed, persistent across restarts)
- **File Storage**: Local filesystem (dev) → Supabase Storage / Cloudflare R2 (pilot)

### Data Flow Diagram

```
┌──────────────┐     FHIR R4      ┌───────────────────────────────┐
│ Hospital EMR ├──────────────────►│  FHIR Webhook Controller      │
└──────────────┘                   │  POST /api/fhir/discharge      │
                                   └──────────┬────────────────────┘
                                              │
┌──────────────┐   WhatsApp API    ┌──────────▼────────────────────┐
│   Patient    │◄─────────────────►│       WhatsApp Service         │
│  (WhatsApp)  │   (Structured     │  - Dispatch checklists         │
└──────────────┘    Buttons)       │  - Receive responses           │
                                   │  - Send reminders/alerts       │
                                   └──────────┬────────────────────┘
                                              │
┌──────────────┐   REST API        ┌──────────▼────────────────────┐
│  Clinician   ├──────────────────►│       Spring Boot App          │
│  Dashboard   │◄──────────────────│  Controllers + Services        │
│  (React SPA) │   JSON            │  + Quartz Jobs                 │
└──────────────┘                   └──┬────────┬───────────────────┘
                                      │        │
                              ┌───────▼─┐  ┌───▼──────────┐
                              │ Postgres │  │    Redis      │
                              │   16     │  │  (Cache +     │
                              │ (Primary │  │   Sessions)   │
                              │  Store)  │  └──────────────┘
                              └─────────┘
```

---

## 2. Database Schema

### Database: PostgreSQL 16.1

- **ORM**: Spring Data JPA (Hibernate 6.4)
- **Naming Convention**: `snake_case` for tables and columns
- **ID Strategy**: UUID (generated via `uuid_generate_v4()` or Java `UUID.randomUUID()`)
- **Timestamps**: All tables have `created_at`, `updated_at` (managed by Spring Data Auditing)
- **Timezone**: All `TIMESTAMP` columns stored as `TIMESTAMP WITH TIME ZONE` (UTC internally)
- **Audit Columns**: Select tables include `created_by`, `last_modified_by` (via `@CreatedBy`, `@LastModifiedBy`)

### Entity Relationship Diagram

```
┌───────────────┐       ┌───────────────────┐       ┌────────────────────┐
│    users      │       │    patients        │       │  recovery_templates│
│  (clinicians) │       │                   │       │                    │
└──────┬────────┘       └───────┬───────────┘       └────────┬───────────┘
       │                        │                            │
       │ 1:N                    │ 1:N                        │ 1:N
       │                        │                            │
       │    ┌───────────────────▼────────────────────────────▼──┐
       │    │                 episodes                          │
       │    │  (one per surgery per patient)                    │
       │    └──┬──────────┬──────────┬──────────┬──────────────┘
       │       │          │          │          │
       │       │ 1:N      │ 1:N      │ 1:N      │ 1:N
       │       │          │          │          │
       │  ┌────▼────┐ ┌───▼───┐ ┌───▼────┐ ┌───▼────────┐
       │  │ daily_  │ │ risk_ │ │ alerts │ │ wound_     │
       │  │responses│ │scores │ │        │ │ images     │
       │  └─────────┘ └───────┘ └───┬────┘ └────────────┘
       │                            │
       │ 1:N                        │ 1:N
       │                            │
  ┌────▼────────────────┐   ┌──────▼──────────────┐
  │ clinical_audit_log  │   │ escalation_outcomes │
  └─────────────────────┘   └─────────────────────┘

  ┌─────────────────┐   ┌──────────────────┐   ┌───────────────┐
  │  consent_logs   │   │  risk_rules      │   │   sessions    │
  └─────────────────┘   └──────────────────┘   └───────────────┘
```

---

## 3. Tables & Relationships

### Table: `users`

**Purpose**: Stores clinician and admin accounts (surgeons, nurses, admins)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Login email |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hashed password (12 rounds) |
| full_name | VARCHAR(255) | NOT NULL | Display name |
| role | VARCHAR(20) | NOT NULL, CHECK (role IN ('ADMIN', 'SURGEON', 'NURSE')) | User role for RBAC |
| phone | VARCHAR(20) | NULL | Contact phone (for alert notifications) |
| is_active | BOOLEAN | DEFAULT TRUE, NOT NULL | Soft-disable account |
| last_login_at | TIMESTAMP WITH TIME ZONE | NULL | Last successful login |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Account creation |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last modification |

**Indexes**:
- `idx_users_email` UNIQUE ON (email)
- `idx_users_role` ON (role)

**Constraints**:
- Email must be valid format (validated in application via `@Email`)
- Password must be BCrypt-hashed with 12 rounds (enforced in service layer)
- Role must be one of ADMIN, SURGEON, NURSE

---

### Table: `patients`

**Purpose**: Stores patient demographic information (persists across episodes)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| full_name | VARCHAR(255) | NOT NULL | Patient name |
| age | INTEGER | NOT NULL, CHECK (age > 0 AND age < 150) | Age at registration |
| gender | VARCHAR(10) | NULL | Gender (M/F/Other) |
| phone_primary | VARCHAR(20) | NOT NULL | Patient WhatsApp number |
| phone_caregiver | VARCHAR(20) | NULL | Caregiver WhatsApp number |
| preferred_language | VARCHAR(10) | DEFAULT 'en', NOT NULL | Language code (en, hi, ta, etc.) |
| hospital_mrn | VARCHAR(100) | NULL | Hospital Medical Record Number |
| fhir_patient_id | VARCHAR(255) | NULL | FHIR resource reference |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Registration timestamp |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last update |

**Indexes**:
- `idx_patients_phone_primary` ON (phone_primary)
- `idx_patients_hospital_mrn` ON (hospital_mrn)
- `idx_patients_fhir_patient_id` ON (fhir_patient_id)

**Constraints**:
- Phone format validated in application (Indian mobile: 10 digits)
- One patient may have multiple episodes (re-admissions)

---

### Table: `recovery_templates`

**Purpose**: Stores surgery-specific recovery monitoring templates (TKR, THR, ACL)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| surgery_type | VARCHAR(50) | UNIQUE, NOT NULL | e.g., TKR, THR, ACL |
| display_name | VARCHAR(255) | NOT NULL | Human-readable name |
| checklist_config | JSONB | NOT NULL | Day-by-day checklist structure |
| milestone_config | JSONB | NOT NULL | Surgery-specific milestones per day |
| mandatory_image_days | INTEGER[] | DEFAULT '{3,5}', NOT NULL | Days requiring wound image |
| monitoring_days | INTEGER | DEFAULT 14, NOT NULL | Total monitoring period |
| is_active | BOOLEAN | DEFAULT TRUE, NOT NULL | Whether template is available |
| created_by | UUID | FOREIGN KEY → users(id), NOT NULL | Admin who created |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last modification |

**Indexes**:
- `idx_templates_surgery_type` UNIQUE ON (surgery_type)
- `idx_templates_is_active` ON (is_active)

**Relationships**:
- `created_by` → `users.id` (many-to-one)
- One template → many episodes

---

### Table: `episodes`

**Purpose**: One monitoring episode per surgery per patient (supports re-admissions without history mixing)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| patient_id | UUID | FOREIGN KEY → patients(id) ON DELETE CASCADE, NOT NULL | Patient reference |
| template_id | UUID | FOREIGN KEY → recovery_templates(id), NOT NULL | Assigned template |
| primary_surgeon_id | UUID | FOREIGN KEY → users(id), NOT NULL | Primary surgeon for alerts |
| secondary_clinician_id | UUID | FOREIGN KEY → users(id), NULL | Fallback clinician for escalation |
| surgery_date | DATE | NOT NULL | Date of surgery |
| discharge_date | DATE | NOT NULL | Date of discharge |
| current_day | INTEGER | DEFAULT 0, NOT NULL | Current post-op day (0 = discharge) |
| status | VARCHAR(20) | DEFAULT 'ACTIVE', NOT NULL, CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED')) | Episode lifecycle |
| pain_score_discharge | INTEGER | NOT NULL, CHECK (pain_score_discharge >= 0 AND pain_score_discharge <= 10) | Baseline pain at discharge |
| swelling_level_discharge | VARCHAR(20) | NOT NULL, CHECK (swelling_level_discharge IN ('NONE', 'MILD', 'MODERATE', 'SEVERE')) | Baseline swelling at discharge |
| consent_status | VARCHAR(20) | DEFAULT 'PENDING', NOT NULL | PENDING, GRANTED, DECLINED |
| consent_timestamp | TIMESTAMP WITH TIME ZONE | NULL | When consent was given |
| checklist_time | TIME | DEFAULT '09:00', NOT NULL | Daily checklist dispatch time |
| timezone | VARCHAR(50) | DEFAULT 'Asia/Kolkata', NOT NULL | Hospital timezone for scheduling |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Episode creation |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last update |

**Indexes**:
- `idx_episodes_patient_id` ON (patient_id)
- `idx_episodes_primary_surgeon_id` ON (primary_surgeon_id)
- `idx_episodes_status` ON (status)
- `idx_episodes_discharge_date` ON (discharge_date)
- `idx_episodes_status_current_day` ON (status, current_day) — for active episode queries

**Relationships**:
- `patient_id` → `patients.id` (many-to-one)
- `template_id` → `recovery_templates.id` (many-to-one)
- `primary_surgeon_id` → `users.id` (many-to-one)
- `secondary_clinician_id` → `users.id` (many-to-one, nullable)
- One episode → many daily_responses, risk_scores, alerts, wound_images

**Constraints**:
- Baseline fields (`pain_score_discharge`, `swelling_level_discharge`) are required — enrollment cannot complete without them
- `primary_surgeon_id` is required

---

### Table: `daily_responses`

**Purpose**: Stores structured daily checklist responses from patient/caregiver

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| episode_id | UUID | FOREIGN KEY → episodes(id) ON DELETE CASCADE, NOT NULL | Parent episode |
| day_number | INTEGER | NOT NULL, CHECK (day_number >= 1 AND day_number <= 30) | Post-op day |
| responder_type | VARCHAR(20) | NOT NULL, CHECK (responder_type IN ('PATIENT', 'CAREGIVER')) | Who responded |
| pain_score | INTEGER | NULL, CHECK (pain_score >= 0 AND pain_score <= 10) | Pain level (0–10) |
| swelling_level | VARCHAR(20) | NULL, CHECK (swelling_level IN ('NONE', 'MILD', 'MODERATE', 'SEVERE')) | Swelling severity |
| fever_level | VARCHAR(20) | NULL, CHECK (fever_level IN ('NO_FEVER', 'BELOW_100', '100_TO_102', 'ABOVE_102')) | Fever range |
| dvt_symptoms | VARCHAR(100)[] | NULL | Array: CALF_PAIN, SWELLING, BREATHLESSNESS |
| mobility_achieved | BOOLEAN | NULL | Surgery-specific milestone met? |
| medication_adherence | VARCHAR(20) | NULL, CHECK (medication_adherence IN ('TOOK_ALL', 'MISSED_SOME', 'DIDNT_TAKE')) | Medication status |
| completion_status | VARCHAR(20) | DEFAULT 'PENDING', NOT NULL | PENDING, PARTIAL, COMPLETED |
| emergency_override | BOOLEAN | DEFAULT FALSE, NOT NULL | Was emergency override triggered? |
| response_started_at | TIMESTAMP WITH TIME ZONE | NULL | When first answer received |
| response_completed_at | TIMESTAMP WITH TIME ZONE | NULL | When last answer received |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Record creation |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last update |

**Indexes**:
- `idx_daily_responses_episode_id` ON (episode_id)
- `idx_daily_responses_episode_day` UNIQUE ON (episode_id, day_number) — one response set per day
- `idx_daily_responses_completion` ON (completion_status)
- `idx_daily_responses_created_at` ON (created_at)

**Relationships**:
- `episode_id` → `episodes.id` (many-to-one)

---

### Table: `risk_scores`

**Purpose**: Stores calculated risk scores per patient per day with rule version snapshot

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| episode_id | UUID | FOREIGN KEY → episodes(id) ON DELETE CASCADE, NOT NULL | Parent episode |
| day_number | INTEGER | NOT NULL | Post-op day |
| composite_score | INTEGER | NOT NULL, CHECK (composite_score >= 0 AND composite_score <= 100) | Overall risk (0–100) |
| risk_level | VARCHAR(10) | NOT NULL, CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')) | Classification |
| contributing_factors | JSONB | NOT NULL | Which rules triggered and their weights |
| trajectory | VARCHAR(20) | NULL, CHECK (trajectory IN ('IMPROVING', 'STABLE', 'WORSENING')) | 3-day trend |
| rule_version_id | VARCHAR(50) | NOT NULL | Version identifier of rule set used |
| rule_set_snapshot | JSONB | NOT NULL | Full rule set at calculation time |
| calculated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | When calculated |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Record creation |

**Indexes**:
- `idx_risk_scores_episode_id` ON (episode_id)
- `idx_risk_scores_episode_day` ON (episode_id, day_number)
- `idx_risk_scores_risk_level` ON (risk_level)
- `idx_risk_scores_composite` ON (composite_score DESC)

**Relationships**:
- `episode_id` → `episodes.id` (many-to-one)

---

### Table: `alerts`

**Purpose**: Tracks alerts generated by the risk engine and their resolution

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| episode_id | UUID | FOREIGN KEY → episodes(id) ON DELETE CASCADE, NOT NULL | Parent episode |
| risk_score_id | UUID | FOREIGN KEY → risk_scores(id), NULL | Triggering risk score |
| alert_type | VARCHAR(30) | NOT NULL, CHECK (alert_type IN ('HIGH_RISK', 'NON_RESPONSE', 'EMERGENCY_OVERRIDE', 'CONSENT_TIMEOUT')) | Type |
| severity | VARCHAR(10) | NOT NULL, CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')) | Alert severity |
| assigned_to | UUID | FOREIGN KEY → users(id), NOT NULL | Clinician to notify |
| status | VARCHAR(20) | DEFAULT 'PENDING', NOT NULL, CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'RESOLVED', 'EXPIRED', 'CANCELLED')) | Lifecycle |
| acknowledged_at | TIMESTAMP WITH TIME ZONE | NULL | When clinician viewed |
| resolved_at | TIMESTAMP WITH TIME ZONE | NULL | When action taken |
| escalation_outcome | VARCHAR(50) | NULL, CHECK (escalation_outcome IN ('OPD_SCHEDULED', 'TELEPHONIC_ADVICE', 'MEDICATION_ADJUSTED', 'ER_REFERRAL', 'FALSE_POSITIVE')) | Resolution action |
| escalation_notes | TEXT | NULL | Clinician notes |
| auto_forwarded | BOOLEAN | DEFAULT FALSE, NOT NULL | Was this auto-forwarded to secondary? |
| sla_deadline | TIMESTAMP WITH TIME ZONE | NULL | Escalation SLA deadline |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Alert creation |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last update |

**Indexes**:
- `idx_alerts_episode_id` ON (episode_id)
- `idx_alerts_assigned_to` ON (assigned_to)
- `idx_alerts_status` ON (status)
- `idx_alerts_type_status` ON (alert_type, status)
- `idx_alerts_created_at` ON (created_at DESC)
- `idx_alerts_sla_deadline` ON (sla_deadline) WHERE status = 'PENDING'

**Relationships**:
- `episode_id` → `episodes.id` (many-to-one)
- `risk_score_id` → `risk_scores.id` (many-to-one, nullable)
- `assigned_to` → `users.id` (many-to-one)

---

### Table: `wound_images`

**Purpose**: Stores wound image metadata and storage references

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| episode_id | UUID | FOREIGN KEY → episodes(id) ON DELETE CASCADE, NOT NULL | Parent episode |
| day_number | INTEGER | NOT NULL | Post-op day of upload |
| storage_path | TEXT | NOT NULL | Path or object key in storage |
| storage_provider | VARCHAR(20) | NOT NULL, CHECK (storage_provider IN ('LOCAL', 'SUPABASE', 'CLOUDFLARE_R2')) | Where stored |
| file_size_bytes | BIGINT | NOT NULL | Original file size |
| content_type | VARCHAR(50) | NOT NULL | MIME type (image/jpeg, image/png) |
| is_mandatory | BOOLEAN | NOT NULL | Was this a required upload day? |
| encrypted | BOOLEAN | DEFAULT TRUE, NOT NULL | Application-level encryption applied? |
| retention_expires_at | TIMESTAMP WITH TIME ZONE | NOT NULL | 3-year retention deadline |
| uploaded_by | VARCHAR(20) | NOT NULL, CHECK (uploaded_by IN ('PATIENT', 'CAREGIVER')) | Who uploaded |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Upload timestamp |

**Indexes**:
- `idx_wound_images_episode_id` ON (episode_id)
- `idx_wound_images_episode_day` ON (episode_id, day_number)
- `idx_wound_images_retention` ON (retention_expires_at) — for cleanup job

**Constraints**:
- `retention_expires_at` = `created_at` + 3 years (set in application)
- Max file size: 10 MB (enforced in application)
- Allowed types: image/jpeg, image/png only

---

### Table: `consent_logs`

**Purpose**: Tracks digital consent capture for DPDP Act 2023 compliance

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| episode_id | UUID | FOREIGN KEY → episodes(id) ON DELETE CASCADE, NOT NULL | Parent episode |
| patient_id | UUID | FOREIGN KEY → patients(id), NOT NULL | Patient reference |
| consent_type | VARCHAR(50) | NOT NULL | e.g., MONITORING, IMAGE_SHARING, DATA_STORAGE |
| status | VARCHAR(20) | NOT NULL, CHECK (status IN ('GRANTED', 'DECLINED', 'REVOKED')) | Consent status |
| method | VARCHAR(20) | NOT NULL, CHECK (method IN ('WHATSAPP', 'MANUAL', 'VERBAL')) | How consent captured |
| consent_text | TEXT | NOT NULL | Exact text shown to patient |
| ip_address | VARCHAR(45) | NULL | If captured via web |
| granted_at | TIMESTAMP WITH TIME ZONE | NULL | When consent given |
| revoked_at | TIMESTAMP WITH TIME ZONE | NULL | If consent revoked |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Record creation |

**Indexes**:
- `idx_consent_logs_episode_id` ON (episode_id)
- `idx_consent_logs_patient_id` ON (patient_id)
- `idx_consent_logs_status` ON (status)

**Constraints**:
- Consent log entries are immutable (no UPDATE allowed — revocation creates a new entry)

**Relationships**:
- `episode_id` → `episodes.id` (many-to-one)
- `patient_id` → `patients.id` (many-to-one)

---

### Table: `clinical_audit_log`

**Purpose**: Immutable audit trail for every clinician action (NABH compliance)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| user_id | UUID | FOREIGN KEY → users(id), NOT NULL | Clinician who acted |
| episode_id | UUID | FOREIGN KEY → episodes(id), NULL | Related episode (if applicable) |
| action | VARCHAR(50) | NOT NULL | CALL, ESCALATE, MARK_REVIEWED, VIEW_IMAGE, LOGIN, LOGOUT, etc. |
| resource_type | VARCHAR(50) | NULL | Entity type (PATIENT, ALERT, TEMPLATE, etc.) |
| resource_id | UUID | NULL | ID of affected resource |
| risk_score_at_action | INTEGER | NULL | Patient risk score at time of action |
| details | JSONB | NULL | Additional context (outcome, notes, changes) |
| ip_address | VARCHAR(45) | NULL | Clinician IP address |
| user_agent | TEXT | NULL | Browser/device info |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Action timestamp |

**Indexes**:
- `idx_audit_user_id` ON (user_id)
- `idx_audit_episode_id` ON (episode_id)
- `idx_audit_action` ON (action)
- `idx_audit_created_at` ON (created_at DESC)

**Constraints**:
- **IMMUTABLE**: No UPDATE or DELETE operations allowed on this table
- Enforced via database trigger or application-level policy
- Spring Data Auditing: `@CreatedBy`, `@CreatedDate` annotations

---

### Table: `risk_rules`

**Purpose**: Stores configurable risk scoring rules (YAML-backed, admin-editable)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| rule_name | VARCHAR(100) | UNIQUE, NOT NULL | e.g., FEVER_HIGH, DVT_SYMPTOMS |
| condition_expression | TEXT | NOT NULL | Rule condition (YAML/DSL) |
| risk_level | VARCHAR(10) | NOT NULL, CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')) | Output risk level |
| weight | INTEGER | NOT NULL, CHECK (weight >= 0 AND weight <= 100) | Score contribution weight |
| is_active | BOOLEAN | DEFAULT TRUE, NOT NULL | Toggle rule on/off |
| version | INTEGER | DEFAULT 1, NOT NULL | Rule version (incremented on edit) |
| created_by | UUID | FOREIGN KEY → users(id), NOT NULL | Admin who created |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last modification |

**Indexes**:
- `idx_risk_rules_rule_name` UNIQUE ON (rule_name)
- `idx_risk_rules_is_active` ON (is_active)

**Relationships**:
- `created_by` → `users.id` (many-to-one)

---

### Table: `sessions`

**Purpose**: Track active user sessions for JWT refresh token management

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| user_id | UUID | FOREIGN KEY → users(id) ON DELETE CASCADE, NOT NULL | Session owner |
| refresh_token_hash | VARCHAR(255) | UNIQUE, NOT NULL | Hashed refresh token |
| user_agent | TEXT | NULL | Browser/device info |
| ip_address | VARCHAR(45) | NULL | Login IP address |
| expires_at | TIMESTAMP WITH TIME ZONE | NOT NULL | Token expiration (7 days from creation) |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Session start |

**Indexes**:
- `idx_sessions_user_id` ON (user_id)
- `idx_sessions_refresh_token_hash` ON (refresh_token_hash)
- `idx_sessions_expires_at` ON (expires_at)

**Cleanup**:
- Quartz scheduled job removes expired sessions daily

---

### Table: `hospital_settings`

**Purpose**: Hospital-level configuration (timezone, checklist time, contact info)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | Unique identifier |
| hospital_name | VARCHAR(255) | NOT NULL | Display name |
| timezone | VARCHAR(50) | DEFAULT 'Asia/Kolkata', NOT NULL | Scheduling timezone |
| default_checklist_time | TIME | DEFAULT '09:00', NOT NULL | Default dispatch time |
| emergency_phone | VARCHAR(20) | NULL | Hospital emergency number |
| dashboard_refresh_interval_min | INTEGER | DEFAULT 5, NOT NULL | Dashboard auto-refresh interval |
| escalation_sla_hours | INTEGER | DEFAULT 2, NOT NULL | Hours before alert auto-forwards |
| created_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Creation |
| updated_at | TIMESTAMP WITH TIME ZONE | DEFAULT NOW(), NOT NULL | Last update |

---

## 4. API Endpoints

### Base URL

- **Development**: `http://localhost:8080/api/v1`
- **Pilot**: `https://api.orthowatch.in/api/v1`

### Authentication Endpoints

#### POST /api/v1/auth/login

**Purpose**: Authenticate clinician and issue JWT tokens

**Request Body**:
```json
{
  "email": "dr.arjun@hospital.in",
  "password": "SecurePass123!"
}
```

**Validation**:
- email: valid format, required
- password: required, min 8 characters

**Response (200)**:
```json
{
  "user": {
    "id": "uuid",
    "email": "dr.arjun@hospital.in",
    "fullName": "Dr. Arjun Rao",
    "role": "SURGEON"
  },
  "accessToken": "eyJhbGciOiJIUzI1..."
}
```

**Cookies Set**:
- `access_token`: HTTP-only, Secure, SameSite=Strict, Max-Age=1800 (30 min)
- `refresh_token`: HTTP-only, Secure, SameSite=Strict, Max-Age=604800 (7 days)

**Errors**:
- 401: Invalid credentials
- 429: Too many login attempts (rate limited)

**Side Effects**:
- Updates `last_login_at` on user record
- Creates session record in `sessions` table
- Creates audit log entry: `LOGIN`

---

#### POST /api/v1/auth/refresh

**Purpose**: Get new access token using refresh token cookie

**Cookies Required**: `refresh_token`

**Response (200)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1..."
}
```

**Cookies Set**:
- `access_token`: New HTTP-only cookie with fresh 30-min expiry

**Errors**:
- 401: Invalid or expired refresh token

---

#### POST /api/v1/auth/logout

**Purpose**: Invalidate session and clear tokens

**Authentication**: Required

**Response (200)**:
```json
{
  "message": "Logged out successfully"
}
```

**Side Effects**:
- Deletes session from `sessions` table
- Clears `access_token` and `refresh_token` cookies
- Creates audit log entry: `LOGOUT`

---

### Dashboard Endpoints

#### GET /api/v1/dashboard/top-risk

**Purpose**: Get Top 10 highest-risk active patients for the logged-in surgeon

**Authentication**: Required (SURGEON, NURSE)

**Query Parameters**:
- `limit`: Max patients (default: 10, max: 20)

**Response (200)**:
```json
{
  "patients": [
    {
      "episodeId": "uuid",
      "patientName": "Lakshmi Devi",
      "surgeryType": "TKR",
      "dayPostOp": 5,
      "riskScore": 75,
      "riskLevel": "HIGH",
      "trajectory": "WORSENING",
      "isNewToday": true,
      "hasRespondedToday": true,
      "lastResponseAt": "2026-02-18T09:45:00+05:30",
      "trendData": [30, 45, 52, 60, 75],
      "latestDvtSymptoms": [],
      "woundImageToday": true
    }
  ],
  "nonResponsiveToday": [
    {
      "episodeId": "uuid",
      "patientName": "Ramesh Kumar",
      "surgeryType": "THR",
      "dayPostOp": 3,
      "hoursSinceLastResponse": 10
    }
  ],
  "lastRefreshed": "2026-02-18T10:00:00+05:30"
}
```

**Caching**:
- Cache key: `dashboard:top-risk:surgeon:{userId}`
- TTL: 5 minutes
- Invalidation: New daily_response received, risk_score updated, alert status changed

**Errors**:
- 401: Not authenticated
- 403: Insufficient role

---

### Patient Endpoints

#### GET /api/v1/patients

**Purpose**: List all active patients with filters

**Authentication**: Required (SURGEON, NURSE)

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Items per page (default: 20, max: 50)
- `surgeryType`: Filter by TKR, THR, ACL
- `riskLevel`: Filter by LOW, MEDIUM, HIGH
- `status`: Filter by ACTIVE, COMPLETED
- `responseStatus`: RESPONDED, PENDING, NON_RESPONSIVE
- `sort`: `riskScore,desc` | `dayPostOp,asc` | `patientName,asc`

**Response (200)**:
```json
{
  "content": [
    {
      "episodeId": "uuid",
      "patientId": "uuid",
      "patientName": "Lakshmi Devi",
      "age": 64,
      "surgeryType": "TKR",
      "surgeryDate": "2026-02-13",
      "dayPostOp": 5,
      "riskScore": 75,
      "riskLevel": "HIGH",
      "trajectory": "WORSENING",
      "responseStatus": "COMPLETED",
      "primarySurgeon": "Dr. Arjun Rao"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

#### GET /api/v1/patients/{episodeId}

**Purpose**: Detailed patient view with full episode data

**Authentication**: Required (SURGEON, NURSE)

**Response (200)**:
```json
{
  "episode": {
    "id": "uuid",
    "status": "ACTIVE",
    "surgeryType": "TKR",
    "surgeryDate": "2026-02-13",
    "dischargeDate": "2026-02-15",
    "currentDay": 5,
    "primarySurgeon": { "id": "uuid", "name": "Dr. Arjun Rao" },
    "secondaryClinician": { "id": "uuid", "name": "Nurse Priya" }
  },
  "patient": {
    "id": "uuid",
    "fullName": "Lakshmi Devi",
    "age": 64,
    "phone": "+9198XXXXXXXX",
    "caregiverPhone": "+9199XXXXXXXX",
    "language": "hi"
  },
  "baseline": {
    "painScoreDischarge": 6,
    "swellingLevelDischarge": "MODERATE"
  },
  "dailyResponses": [
    {
      "dayNumber": 1,
      "painScore": 5,
      "swellingLevel": "MODERATE",
      "feverLevel": "NO_FEVER",
      "dvtSymptoms": [],
      "mobilityAchieved": false,
      "medicationAdherence": "TOOK_ALL",
      "completionStatus": "COMPLETED",
      "respondedAt": "2026-02-16T09:30:00+05:30"
    }
  ],
  "riskScores": [
    {
      "dayNumber": 1,
      "compositeScore": 30,
      "riskLevel": "LOW",
      "trajectory": null,
      "contributingFactors": { "swelling_moderate": 10, "pain_level_5": 5 }
    }
  ],
  "woundImages": [
    {
      "id": "uuid",
      "dayNumber": 3,
      "imageUrl": "/api/v1/images/uuid",
      "isMandatory": true,
      "uploadedAt": "2026-02-18T10:15:00+05:30"
    }
  ],
  "activeAlerts": [],
  "auditLog": [
    {
      "action": "MARK_REVIEWED",
      "performedBy": "Dr. Arjun Rao",
      "riskScoreAtAction": 30,
      "timestamp": "2026-02-16T14:00:00+05:30"
    }
  ]
}
```

**Side Effects**:
- Creates audit log entry: `VIEW_PATIENT`

**Caching**:
- Cache key: `patient:detail:{episodeId}`
- TTL: 2 minutes
- Invalidation: New response, risk score update, alert change

---

### Enrollment Endpoints

#### POST /api/v1/enrollments

**Purpose**: Enroll patient into monitoring system (manual or FHIR-triggered)

**Authentication**: Required (NURSE, ADMIN)

**Request Body**:
```json
{
  "patientName": "Lakshmi Devi",
  "age": 64,
  "gender": "F",
  "phonePrimary": "+919876543210",
  "phoneCaregiver": "+919876543211",
  "preferredLanguage": "hi",
  "hospitalMrn": "MRN-2026-001",
  "surgeryType": "TKR",
  "surgeryDate": "2026-02-13",
  "dischargeDate": "2026-02-15",
  "primarySurgeonId": "uuid",
  "secondaryClinicianId": "uuid",
  "painScoreDischarge": 6,
  "swellingLevelDischarge": "MODERATE"
}
```

**Validation**:
- patientName: 2–255 characters, required
- phonePrimary: valid Indian mobile format, required
- surgeryType: must match an active template
- surgeryDate, dischargeDate: valid dates, surgery ≤ discharge
- primarySurgeonId: must reference existing user with SURGEON role
- painScoreDischarge: 0–10, required
- swellingLevelDischarge: NONE/MILD/MODERATE/SEVERE, required

**Response (201)**:
```json
{
  "episodeId": "uuid",
  "patientId": "uuid",
  "status": "ACTIVE",
  "consentStatus": "PENDING",
  "message": "Patient enrolled. Consent message sent via WhatsApp."
}
```

**Errors**:
- 400: Validation failed
- 404: Surgeon not found, Template not found
- 409: Patient already has active episode for this surgery type

**Side Effects**:
- Creates `patients` record (or reuses existing by phone)
- Creates `episodes` record
- Creates `consent_logs` entry (PENDING)
- Sends WhatsApp consent message via WhatsApp API
- Schedules consent timeout job (24 hours) via Quartz
- Creates audit log entry: `ENROLL_PATIENT`

---

### Alert & Escalation Endpoints

#### POST /api/v1/alerts/{alertId}/acknowledge

**Purpose**: Clinician acknowledges alert

**Authentication**: Required (SURGEON, NURSE)

**Response (200)**:
```json
{
  "alertId": "uuid",
  "status": "ACKNOWLEDGED",
  "acknowledgedAt": "2026-02-18T10:30:00+05:30"
}
```

**Side Effects**:
- Updates alert status to ACKNOWLEDGED
- Creates audit log entry: `ACKNOWLEDGE_ALERT`

---

#### POST /api/v1/alerts/{alertId}/resolve

**Purpose**: Clinician resolves alert with outcome selection

**Authentication**: Required (SURGEON, NURSE)

**Request Body**:
```json
{
  "escalationOutcome": "OPD_SCHEDULED",
  "notes": "Patient advised to visit OPD tomorrow. Wound shows mild redness."
}
```

**Validation**:
- escalationOutcome: required, must be one of OPD_SCHEDULED, TELEPHONIC_ADVICE, MEDICATION_ADJUSTED, ER_REFERRAL, FALSE_POSITIVE
- notes: optional, max 2000 characters

**Response (200)**:
```json
{
  "alertId": "uuid",
  "status": "RESOLVED",
  "escalationOutcome": "OPD_SCHEDULED",
  "resolvedAt": "2026-02-18T10:45:00+05:30"
}
```

**Side Effects**:
- Updates alert status to RESOLVED
- Stores escalation outcome and notes
- KPI events: `escalation_triggered`, `escalation_resolved`
- Creates audit log entry: `RESOLVE_ALERT`
- Calculates SLA compliance: `resolved_at - created_at` vs `escalation_sla_hours`

---

#### POST /api/v1/patients/{episodeId}/mark-reviewed

**Purpose**: Clinician marks patient as reviewed (removes from Top 10)

**Authentication**: Required (SURGEON, NURSE)

**Response (200)**:
```json
{
  "message": "Patient marked as reviewed",
  "reviewedAt": "2026-02-18T11:00:00+05:30"
}
```

**Side Effects**:
- Creates audit log entry: `MARK_REVIEWED` (with risk_score_at_action)
- Invalidates dashboard cache for surgeon

---

### Admin Endpoints

#### GET /api/v1/admin/templates

**Purpose**: List all recovery templates

**Authentication**: Required (ADMIN)

**Response (200)**:
```json
{
  "templates": [
    {
      "id": "uuid",
      "surgeryType": "TKR",
      "displayName": "Total Knee Replacement",
      "isActive": true,
      "monitoringDays": 14,
      "mandatoryImageDays": [3, 5]
    }
  ]
}
```

---

#### PUT /api/v1/admin/templates/{templateId}

**Purpose**: Update recovery template

**Authentication**: Required (ADMIN)

**Request Body**:
```json
{
  "displayName": "Total Knee Replacement",
  "checklistConfig": { "day1": [...], "day2": [...] },
  "milestoneConfig": { "day1": "Bend knee 30°", "day7": "Bend knee 90°" },
  "mandatoryImageDays": [3, 5],
  "monitoringDays": 14,
  "isActive": true
}
```

**Response (200)**:
```json
{
  "id": "uuid",
  "message": "Template updated successfully"
}
```

**Errors**:
- 400: Invalid config structure
- 404: Template not found
- 409: Concurrent modification (optimistic locking)

---

#### GET /api/v1/admin/rules

**Purpose**: List all risk scoring rules

**Authentication**: Required (ADMIN)

---

#### PUT /api/v1/admin/rules/{ruleId}

**Purpose**: Update a risk scoring rule

**Authentication**: Required (ADMIN)

**Request Body**:
```json
{
  "conditionExpression": "fever_temp > 100",
  "riskLevel": "HIGH",
  "weight": 30,
  "isActive": true
}
```

**Side Effects**:
- Increments rule `version` number
- Creates audit log entry: `UPDATE_RULE`

---

### WhatsApp Webhook Endpoints

#### POST /api/v1/webhook/whatsapp

**Purpose**: Receive inbound messages and status updates from WhatsApp Business API

**Authentication**: Webhook verification token (WhatsApp-specific)

**Incoming Payload** (from Meta):
```json
{
  "entry": [{
    "changes": [{
      "value": {
        "messages": [{
          "from": "919876543210",
          "type": "interactive",
          "interactive": {
            "type": "button_reply",
            "button_reply": { "id": "pain_moderate", "title": "4–6 Moderate" }
          }
        }]
      }
    }]
  }]
}
```

**Processing**:
- Map phone number → patient → active episode
- Parse button reply → update daily_response field
- If all required fields collected → trigger risk engine
- If emergency symptom → trigger emergency override flow

**Errors**:
- 200: Always return 200 to WhatsApp (even on processing error — to prevent retries)
- Internal errors logged to Sentry

---

#### GET /api/v1/webhook/whatsapp

**Purpose**: WhatsApp webhook verification (one-time setup)

**Query Parameters**:
- `hub.mode`: Must be "subscribe"
- `hub.verify_token`: Must match `WHATSAPP_VERIFY_TOKEN`
- `hub.challenge`: Echo back to verify

---

### FHIR Integration Endpoints

#### POST /api/v1/fhir/discharge

**Purpose**: Receive FHIR R4 discharge event from hospital EMR

**Authentication**: API key (hospital-specific)

**Request Body**: FHIR R4 `Encounter` resource (discharge event)

**Processing**:
- Parse patient demographics from FHIR resource
- Create patient record (or match existing)
- Create pending episode with default template
- Notify nurse for enrollment validation

**Errors**:
- 400: Invalid FHIR resource
- 422: Unsupported surgery type

---

### Image Endpoints

#### GET /api/v1/images/{imageId}

**Purpose**: Serve wound image (with decryption)

**Authentication**: Required (SURGEON, NURSE)

**Response**: Image binary with appropriate Content-Type header

**Side Effects**:
- Creates audit log entry: `VIEW_IMAGE`

---

## 5. Authentication & Authorization

### JWT Token Structure

**Access Token** (30-minute expiry):
```json
{
  "sub": "user_uuid",
  "email": "dr.arjun@hospital.in",
  "role": "SURGEON",
  "fullName": "Dr. Arjun Rao",
  "iat": 1739856000,
  "exp": 1739857800
}
```

**Refresh Token** (7-day expiry):
```json
{
  "sub": "user_uuid",
  "sessionId": "session_uuid",
  "iat": 1739856000,
  "exp": 1740460800
}
```

### Authorization Levels

#### Public Routes (No auth required)
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- GET /api/v1/webhook/whatsapp (verification)
- POST /api/v1/webhook/whatsapp (inbound messages)
- GET /api/v1/actuator/health

#### Authenticated Routes (Valid access token)
- GET /api/v1/dashboard/*
- GET /api/v1/patients/*
- POST /api/v1/patients/*/mark-reviewed
- POST /api/v1/alerts/*/acknowledge
- POST /api/v1/alerts/*/resolve
- POST /api/v1/auth/logout
- GET /api/v1/images/*

#### Role-Restricted Routes
- POST /api/v1/enrollments — NURSE, ADMIN
- GET/PUT /api/v1/admin/templates/* — ADMIN
- GET/PUT /api/v1/admin/rules/* — ADMIN
- */admin/users — ADMIN

#### API Key Routes (Machine-to-Machine)
- POST /api/v1/fhir/discharge — Hospital EMR

### Password Security

- **Hashing**: BCrypt with 12 rounds (via Spring Security's `BCryptPasswordEncoder`)
- **Never** stored in plain text
- **Never** returned in API responses
- **Reset**: Via email verification flow (P1 feature)

### Spring Security Implementation

```java
// SecurityConfig.java — high-level structure
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
    // JWT filter → extracts token from cookie or Authorization header
    // Role-based: @PreAuthorize("hasRole('SURGEON')") on controller methods
    // CORS: Only frontend origin allowed
    // CSRF: Disabled (stateless JWT)
    // Session: STATELESS
}
```

---

## 6. Data Validation Rules

### Phone Number Validation (Indian Mobile)

```java
// Pattern: 10-digit Indian mobile, optional +91 prefix
// E.164 format stored: +919876543210
private static final Pattern PHONE_REGEX = Pattern.compile("^\\+?91?[6-9]\\d{9}$");
```

### Password Requirements

```java
// Minimum requirements:
// - Length: 8–128 characters
// - At least 1 uppercase letter
// - At least 1 lowercase letter
// - At least 1 number
// - At least 1 special character (!@#$%^&*)
```

### Clinical Data Validation

| Field | Type | Validation | Error Message |
|---|---|---|---|
| painScore | Integer | 0–10, required | "Pain score must be between 0 and 10" |
| swellingLevel | Enum | NONE/MILD/MODERATE/SEVERE | "Invalid swelling level" |
| feverLevel | Enum | NO_FEVER/BELOW_100/100_TO_102/ABOVE_102 | "Invalid fever level" |
| dvtSymptoms | Array | CALF_PAIN/SWELLING/BREATHLESSNESS | "Invalid DVT symptom" |
| surgeryType | String | Must match active template | "Unknown surgery type" |
| surgeryDate | Date | Not in the future, within 30 days of today | "Invalid surgery date" |

### Input Sanitization

- Strip HTML tags from all text inputs (Spring's `@SafeHtml` or custom sanitizer)
- JSON request bodies validated via `@Valid` + Bean Validation annotations
- Max field lengths enforced at both application and DB level
- File upload: image/jpeg and image/png only, max 10 MB

---

## 7. Error Handling

### Error Response Format

All errors follow a standardized JSON structure:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "timestamp": "2026-02-18T10:00:00+05:30",
    "path": "/api/v1/enrollments",
    "details": [
      {
        "field": "phonePrimary",
        "message": "Invalid Indian mobile number format"
      },
      {
        "field": "painScoreDischarge",
        "message": "Pain score must be between 0 and 10"
      }
    ]
  }
}
```

### Error Codes

| Error Code | HTTP Status | Description |
|---|---|---|
| VALIDATION_ERROR | 400 | Request body / parameter validation failed |
| UNAUTHORIZED | 401 | Missing or invalid JWT token |
| FORBIDDEN | 403 | Insufficient role/permissions |
| NOT_FOUND | 404 | Resource does not exist |
| CONFLICT | 409 | Duplicate resource / concurrent modification |
| RATE_LIMITED | 429 | Too many requests |
| WHATSAPP_ERROR | 502 | WhatsApp API call failed |
| SERVER_ERROR | 500 | Unexpected internal error |

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // MethodArgumentNotValidException → 400 VALIDATION_ERROR
    // AccessDeniedException → 403 FORBIDDEN
    // EntityNotFoundException → 404 NOT_FOUND
    // OptimisticLockingFailureException → 409 CONFLICT
    // RateLimitExceededException → 429 RATE_LIMITED
    // Exception → 500 SERVER_ERROR (logged to Sentry)
}
```

---

## 8. Caching Strategy

### Cache Layers

1. **Redis** (primary cache — Spring Data Redis with Lettuce)
   - Dashboard API responses
   - User sessions (refresh token lookup)
   - Rate limiting counters
   - Template cache (YAML risk rules)

2. **Application-Level** (in-memory)
   - Risk rule definitions (loaded at startup, refreshed on admin change)
   - Hospital settings (loaded once, refreshed on change)

### Cache Keys

| Cache Purpose | Key Format | TTL |
|---|---|---|
| Dashboard top-risk | `dashboard:top-risk:surgeon:{userId}` | 5 min |
| Patient detail | `patient:detail:{episodeId}` | 2 min |
| Patient list (filtered) | `patients:list:{hash(filters)}` | 5 min |
| Template definition | `template:{surgeryType}` | 24 hours |
| Risk rules (active) | `risk-rules:active` | Until admin change |
| Rate limit (login) | `rate:login:{ip}` | 15 min |
| Rate limit (API) | `rate:api:{userId}` | 1 min |
| Session | `session:{refreshTokenHash}` | 7 days |

### Cache Invalidation

| Event | Invalidated Keys |
|---|---|
| New daily_response | `dashboard:top-risk:surgeon:*`, `patient:detail:{episodeId}` |
| Risk score calculated | `dashboard:top-risk:surgeon:*`, `patient:detail:{episodeId}`, `patients:list:*` |
| Alert status changed | `dashboard:top-risk:surgeon:*`, `patient:detail:{episodeId}` |
| Patient enrolled | `patients:list:*` |
| Template updated | `template:{surgeryType}` |
| Risk rule updated | `risk-rules:active` |

---

## 9. Rate Limiting

### Limits by Endpoint

| Endpoint Category | Limit | Window | Scope |
|---|---|---|---|
| Login (`/auth/login`) | 5 requests | 15 minutes | Per IP |
| Registration (future) | 3 requests | 1 hour | Per IP |
| API (authenticated) | 100 requests | 1 minute | Per user |
| API (public) | 50 requests | 1 minute | Per IP |
| WhatsApp webhook | 500 requests | 1 minute | Global (Meta's rate) |
| File uploads | 10 uploads | 1 hour | Per episode |

### Implementation

- Stored in Redis with sliding window algorithm
- Key format: `rate:{category}:{identifier}`
- Response on limit exceeded: HTTP 429 with `Retry-After` header
- Custom Spring Security filter for login rate limiting
- Custom `@RateLimit` annotation for API endpoints

---

## 10. Database Migrations

### Migration Strategy

- **Tool**: Flyway 10.x (SQL-based migrations)
- **Location**: `src/main/resources/db/migration/`
- **Naming**: `V{version}__{description}.sql` (e.g., `V1__initial_schema.sql`)
- **Policy**: Never edit migrations after deployment — always create new migrations

### Migration Process

```bash
# 1. Create migration file
# src/main/resources/db/migration/V3__add_escalation_sla.sql

# 2. Test locally
./mvnw flyway:migrate

# 3. Run with app (auto-migrates on startup in dev profile)
./mvnw spring-boot:run

# 4. Deploy to pilot (Flyway runs on app startup)
# Neon database auto-migrated

# 5. Rollback (manual — create a compensating migration)
# V4__rollback_escalation_sla.sql
```

### Planned Migrations

| Version | Description |
|---|---|
| V1__initial_schema.sql | All 13 tables, indexes, constraints |
| V2__seed_templates.sql | Default TKR, THR, ACL templates + default risk rules |
| V3__seed_admin_user.sql | Initial admin account (password must be changed on first login) |

---

## 11. Backup & Recovery

### Backup Strategy

- **Pilot (Neon)**: Automated point-in-time recovery, 7-day history (included in free tier)
- **Local Dev**: Manual `pg_dump` as needed
- **Post-Pilot (AWS RDS)**: Daily automated snapshots, 30-day retention

### Recovery Procedure

1. Identify the point-in-time to restore to
2. Use Neon dashboard → "Branching" to create a recovery branch
3. Connect application to recovery branch for validation
4. Verify data integrity (patient count, recent responses, audit log)
5. Swap connection string to recovery branch
6. Monitor for 24 hours, then decommission old branch

### Data Retention

| Data Type | Retention | Policy |
|---|---|---|
| Wound images | 3 years | `retention_expires_at` field, cleanup cron job |
| Daily responses | Indefinite (pilot) | Archival after 1 year post-pilot |
| Audit logs | Indefinite | Never deleted (NABH requirement) |
| Sessions | 7 days | Expired sessions purged daily |
| Risk scores | Indefinite (pilot) | Historical scores needed for trend analysis |

---

## 12. API Versioning

### Current Version: v1

- **URL Prefix**: `/api/v1/`
- All endpoints prefixed with version

### Versioning Strategy

- **URL-based** versioning: `/api/v1/patients`, `/api/v2/patients`
- No version in URL defaults to v1 (redirect)
- All clients must specify version

### Breaking Change Policy

- Breaking changes require new version increment
- Old version supported for minimum 6 months
- Deprecation warnings returned in `X-API-Deprecated` response header
- Migration guide provided in release notes
- Pilot phase: v1 only — versioning infrastructure ready for post-pilot

---

## Appendix: Spring Boot Project Layout

```
backend/src/main/java/com/orthowatch/
├── OrthoWatchApplication.java          # @SpringBootApplication entry point
├── config/
│   ├── SecurityConfig.java             # JWT filter, CORS, role mappings
│   ├── RedisConfig.java                # Redis connection + cache manager
│   ├── QuartzConfig.java               # Quartz scheduler + job store
│   ├── WebConfig.java                  # CORS, interceptors
│   └── OpenApiConfig.java             # Swagger/OpenAPI documentation
├── controller/
│   ├── AuthController.java             # /api/v1/auth/*
│   ├── DashboardController.java        # /api/v1/dashboard/*
│   ├── PatientController.java          # /api/v1/patients/*
│   ├── EnrollmentController.java       # /api/v1/enrollments
│   ├── AlertController.java            # /api/v1/alerts/*
│   ├── AdminController.java            # /api/v1/admin/*
│   ├── WebhookController.java          # /api/v1/webhook/*
│   ├── FhirController.java             # /api/v1/fhir/*
│   └── ImageController.java            # /api/v1/images/*
├── service/
│   ├── AuthService.java
│   ├── RiskEngineService.java
│   ├── WhatsAppService.java
│   ├── ChecklistService.java
│   ├── EnrollmentService.java
│   ├── AlertService.java
│   ├── AuditService.java
│   ├── ImageStorageService.java
│   └── ConsentService.java
├── model/                              # JPA @Entity classes
│   ├── User.java
│   ├── Patient.java
│   ├── Episode.java
│   ├── DailyResponse.java
│   ├── RiskScore.java
│   ├── Alert.java
│   ├── WoundImage.java
│   ├── ConsentLog.java
│   ├── ClinicalAuditLog.java
│   ├── RiskRule.java
│   ├── RecoveryTemplate.java
│   ├── Session.java
│   └── HospitalSettings.java
├── repository/                         # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── PatientRepository.java
│   ├── EpisodeRepository.java
│   ├── DailyResponseRepository.java
│   ├── RiskScoreRepository.java
│   ├── AlertRepository.java
│   └── ... (one per entity)
├── dto/                                # Request/Response DTOs
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── EnrollmentRequest.java
│   │   └── EscalationResolveRequest.java
│   └── response/
│       ├── DashboardResponse.java
│       ├── PatientDetailResponse.java
│       └── ErrorResponse.java
├── mapper/                             # MapStruct mappers
│   ├── PatientMapper.java
│   └── EpisodeMapper.java
├── scheduler/                          # Quartz job classes
│   ├── ChecklistDispatchJob.java
│   ├── ReminderJob.java
│   ├── EscalationJob.java
│   ├── ConsentTimeoutJob.java
│   └── SessionCleanupJob.java
├── rules/                              # Risk rule engine
│   └── RiskRuleEvaluator.java
├── exception/                          # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── RateLimitExceededException.java
│   └── ConsentRequiredException.java
└── util/
    ├── JwtUtil.java
    └── PhoneNumberUtil.java
```
