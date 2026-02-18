# Technology Stack Documentation

## OrthoWatch – Post-Discharge Monitoring System

**Last Updated**: Feb 2026
**Version**: 1.1
**Based on**: OrthoWatch PRD v1.0 + APP_FLOW v1.2
**Team**: Solo developer (learning Java + Maven)
**Budget**: $0 during development — all free tools and services

---

## 1. Stack Overview

### Architecture Pattern
- **Type**: Modular Monolith (single deployable, logically separated modules)
- **Pattern**: MVC with Service Layer (Controller → Service → Repository)
- **Deployment**: Local development → Free-tier cloud for pilot
- **API Style**: REST (JSON) with OpenAPI/Swagger documentation

### Budget Philosophy

> **$0 during development. $0 or near-$0 for pilot deployment.**
> Every tool, service, and dependency below is either open-source (free) or has a free tier sufficient for OrthoWatch's pilot scale (150 concurrent episodes). Paid upgrades are only needed post-pilot if the hospital scales.

### Cost Breakdown

| Component | Development | Pilot (150 episodes) | Paid Alternative (Post-Pilot) |
|---|---|---|---|
| Java 21 | Free (OpenJDK) | Free | Free |
| Spring Boot | Free (Apache 2.0) | Free | Free |
| PostgreSQL | Free (Docker local) | Free (Neon / Supabase) | AWS RDS |
| Redis | Free (Docker local) | Free (Upstash) | AWS ElastiCache |
| File Storage | Free (local disk) | Free (Supabase Storage / Cloudflare R2) | AWS S3 |
| Backend Hosting | localhost | Free (Render / Railway) | AWS EC2 |
| Frontend Hosting | localhost | Free (Vercel / Netlify) | AWS CloudFront |
| CI/CD | N/A | Free (GitHub Actions) | Same |
| Monitoring | Spring Actuator | Free (Sentry free tier) | Sentry paid |
| WhatsApp API | Free (Meta test sandbox) | Free (Meta Cloud API) | Same |
| **Total** | **$0** | **$0** | ~$50-100/mo |

### Architecture Diagram

```
┌─────────────────────────────────────────┐
│              FRONTEND                    │
│  React 18 + Vite + TypeScript 5.3       │
│  Tailwind CSS 3.4 + shadcn/ui          │
│  TanStack Query + Zustand + Recharts   │
│  Hosted: Vercel (free tier)            │
├─────────────────────────────────────────┤
│              API LAYER (REST)            │
│  Spring Boot 3.2 (Java 21 LTS)         │
│  Spring Security 6.2 (JWT + RBAC)      │
│  SpringDoc OpenAPI 2.3 (Swagger UI)    │
│  Hosted: Render (free tier)            │
├─────────────────────────────────────────┤
│              BACKEND SERVICES            │
│  Quartz Scheduler (checklist dispatch)  │
│  WhatsApp Business API client           │
│  HAPI FHIR R4 (EMR integration)         │
│  SnakeYAML (risk rule engine)           │
├─────────────────────────────────────────┤
│              DATA LAYER                  │
│  PostgreSQL 16 (Spring Data JPA)        │
│  Redis 7 (Spring Data Redis)            │
│  Supabase Storage (wound images)        │
│  Flyway 10 (database migrations)        │
├─────────────────────────────────────────┤
│              INFRASTRUCTURE              │
│  Docker (local dev)                     │
│  GitHub Actions (CI/CD — free)          │
│  Spring Actuator + Sentry (free tier)  │
└─────────────────────────────────────────┘
```

---

## 2. Frontend Stack

### Core Framework
- **Framework**: React
- **Version**: 18.2.0
- **Reason**: Component-based architecture, massive ecosystem, shadcn/ui compatibility
- **Documentation**: https://react.dev
- **License**: MIT
- **Cost**: Free

### Build Tool
- **Tool**: Vite
- **Version**: 5.1.0
- **Reason**: Fast HMR, simple config, modern ESM-first build — faster than CRA/Webpack
- **Documentation**: https://vitejs.dev
- **License**: MIT
- **Cost**: Free

### Language
- **Language**: TypeScript
- **Version**: 5.3.3
- **tsconfig**: Strict mode enabled
- **Reason**: Type safety for clinical data structures, better IDE support

### Styling
- **Framework**: Tailwind CSS
- **Version**: 3.4.1
- **Configuration**: `tailwind.config.js` with custom medical color palette
- **Reason**: Utility-first, rapid UI development, consistent design system
- **Documentation**: https://tailwindcss.com/docs
- **License**: MIT
- **Cost**: Free

### UI Components
- **Library**: shadcn/ui
- **Version**: Latest (component-level, not versioned as package)
- **Reason**: Accessible, customizable, clean – ideal for medical dashboard clarity
- **Documentation**: https://ui.shadcn.com
- **License**: MIT
- **Cost**: Free
- **Alternatives Considered**: Material UI (rejected: too opinionated, heavy), Ant Design (rejected: less customizable)

### State Management
- **Library**: Zustand
- **Version**: 4.5.0
- **Reason**: Lightweight, TypeScript-first, minimal boilerplate — suited for solo developer
- **Documentation**: https://github.com/pmndrs/zustand
- **License**: MIT
- **Cost**: Free

### Data Fetching
- **Library**: TanStack Query (React Query)
- **Version**: 5.17.0
- **Reason**: Handles caching, 5-min auto-refresh (dashboard polling), stale-while-revalidate, loading/error states
- **Documentation**: https://tanstack.com/query
- **License**: MIT
- **Cost**: Free

### Forms
- **Library**: React Hook Form
- **Version**: 7.49.0
- **Validation**: Zod 3.22.4
- **Reason**: Enrollment validation screen, escalation outcome modal, risk rule editor
- **Cost**: Free

### Charts
- **Library**: Recharts
- **Version**: 2.12.0
- **Reason**: Pain/swelling trend graphs, risk score history, sparklines on patient cards
- **Documentation**: https://recharts.org
- **License**: MIT
- **Cost**: Free

### HTTP Client
- **Library**: Axios
- **Version**: 1.6.5
- **Interceptors**: JWT token injection, 401 redirect, error normalization
- **Reason**: Request/response interceptors simplify auth handling
- **Cost**: Free

### Icons
- **Library**: Lucide React
- **Version**: 0.312.0
- **Reason**: Clean icon set, tree-shakeable, consistent with shadcn/ui
- **Cost**: Free

---

## 3. Backend Stack

### Runtime & Language
- **Language**: Java
- **Version**: 21 LTS (Long-Term Support until Sept 2031)
- **Distribution**: Eclipse Temurin (Adoptium) — **free, open-source OpenJDK**
- **Reason**: Enterprise standard in Indian hospitals, strong type system, massive talent pool, LTS longevity
- **Documentation**: https://docs.oracle.com/en/java/javase/21/
- **Download**: https://adoptium.net/temurin/releases/?version=21
- **Cost**: Free (do NOT use Oracle JDK — it requires a license)

### Framework
- **Framework**: Spring Boot
- **Version**: 3.2.x
- **Starters Used**:
  - `spring-boot-starter-web` (REST controllers)
  - `spring-boot-starter-data-jpa` (database access)
  - `spring-boot-starter-data-redis` (caching)
  - `spring-boot-starter-security` (auth + RBAC)
  - `spring-boot-starter-validation` (request validation)
  - `spring-boot-starter-actuator` (health checks, metrics)
  - `spring-boot-starter-quartz` (job scheduling)
- **Reason**: Production-ready defaults, auto-configuration, massive community, industry trust
- **Documentation**: https://spring.io/projects/spring-boot
- **License**: Apache 2.0
- **Cost**: Free

### Build Tool
- **Tool**: Apache Maven
- **Version**: 3.9.6
- **Configuration**: `pom.xml` (XML-based — standard for learning Java)
- **Reason**: Industry standard for Java projects, most tutorials and docs use Maven, XML structure is explicit and beginner-friendly
- **Documentation**: https://maven.apache.org/guides/
- **License**: Apache 2.0
- **Cost**: Free
- **Wrapper**: Maven Wrapper (`mvnw`) included — no global install needed

### Database
- **Primary**: PostgreSQL
- **Version**: 16.1
- **ORM**: Spring Data JPA (Hibernate 6.4)
- **Connection Pooling**: HikariCP (Spring Boot default)
- **Reason**: ACID compliance for clinical data, JSONB for rule snapshots, episode-based relational model
- **Documentation**: https://www.postgresql.org/docs/16/
- **Cost**: Free (Docker locally, **Neon free tier** or **Supabase free tier** for pilot)

#### Schema Management
- **Migrations**: Flyway 10.x
- **Seeding**: `data.sql` + custom `@Component` seed runner
- **Backup Strategy**: Neon auto-backups (free tier includes 7-day history)
- **Reason for Flyway over Liquibase**: Simpler SQL-based migrations, lower learning curve for solo developer

#### Free Database Hosting Options (Pilot)

| Provider | Free Tier | Limit | Best For |
|---|---|---|---|
| **Neon** | Free forever | 0.5 GB storage, 1 project | ✅ Recommended — generous, auto-suspend |
| **Supabase** | Free forever | 500 MB, 2 projects | Good — also includes Storage and Auth |
| **Railway** | $5 trial credit | Runs out eventually | Short-term testing only |
| **Local Docker** | Free | Unlimited | Development |

### Caching
- **System**: Redis
- **Version**: 7.2 (local Docker), Upstash (free tier for pilot)
- **Client**: Spring Data Redis (Lettuce driver, default in Spring Boot)
- **Use Cases**: Session storage, dashboard API response caching, rate limiting counters
- **TTL Strategy**: 5 min for dashboard cache, 24 hours for template cache
- **Cost**: Free (Docker locally, **Upstash free tier**: 10K commands/day — sufficient for 150 episodes)
- **Upstash**: https://upstash.com — serverless Redis, no credit card needed

### Authentication & Authorization
- **Strategy**: JWT (JSON Web Tokens)
- **Library**: jjwt (io.jsonwebtoken) 0.12.x
- **Framework**: Spring Security 6.2
- **Password Hashing**: BCrypt (12 rounds) via Spring Security
- **Token Expiry**: Access token (30 min), Refresh token (7 days)
- **Storage**: HTTP-only cookies (access token) — prevents XSS
- **RBAC**: Method-level `@PreAuthorize("hasRole('SURGEON')")` annotations
- **Roles**: ADMIN, SURGEON, NURSE
- **Cost**: Free (all open-source)

### File Storage (Wound Images)
- **Development**: Local filesystem (`./uploads/` directory)
- **Pilot**: **Supabase Storage** (free tier: 1 GB) or **Cloudflare R2** (free tier: 10 GB/mo)
- **Encryption**: Application-level encryption before storage
- **Lifecycle**: 3-year retention managed at application level
- **Cost**: Free
- **Migration Path**: Swap to AWS S3 post-pilot if hospital requires it (same S3-compatible API for R2)

#### Free File Storage Options (Pilot)

| Provider | Free Tier | S3-Compatible | Best For |
|---|---|---|---|
| **Supabase Storage** | 1 GB | Yes (via API) | ✅ Recommended if using Supabase DB |
| **Cloudflare R2** | 10 GB/mo | Yes (full S3 API) | ✅ Recommended for larger image volume |
| **Local filesystem** | Unlimited | N/A | Development only |

### FHIR Integration
- **Library**: HAPI FHIR
- **Version**: 7.0.x
- **Standard**: FHIR R4
- **Use Case**: Receive discharge events from hospital EMR, extract patient demographics
- **Reason**: Industry-standard healthcare interoperability library (HL7-maintained)
- **Documentation**: https://hapifhir.io
- **Fallback**: CSV import for hospitals without FHIR capability
- **License**: Apache 2.0
- **Cost**: Free

### Job Scheduling
- **Library**: Quartz Scheduler (via `spring-boot-starter-quartz`)
- **Version**: 2.3.x
- **Persistence**: JDBC job store (PostgreSQL) — survives restarts
- **Use Cases**:
  - Daily checklist dispatch (hospital-configured timezone, default 9:00 AM IST)
  - 4-hour non-response reminders
  - 8-hour escalation triggers
  - Consent follow-up (24-hour timeout)
- **Reason**: Timezone-aware, persistent, cluster-safe — more robust than `@Scheduled`
- **Cost**: Free

### Risk Rule Engine
- **Config Format**: YAML (loaded via SnakeYAML 2.2)
- **Execution**: Custom Java evaluator — rules are simple IF/THEN conditions
- **Versioning**: Each calculation stores `rule_version_id` + `rule_set_snapshot` (JSONB)
- **Reason**: YAML is human-readable for admin configuration; no need for a full rules engine (Drools) at pilot scale
- **Cost**: Free

### Utility Libraries
- **Lombok**: 1.18.x — Eliminates boilerplate (`@Data`, `@Builder`, `@Slf4j`) — Free
- **MapStruct**: 1.5.x — Type-safe DTO ↔ Entity mapping — Free
- **Jackson**: 2.16.x — JSON serialization (Spring Boot default) — Free
- **SnakeYAML**: 2.2 — YAML parsing for risk rules — Free

---

## 4. DevOps & Infrastructure

### Version Control
- **System**: Git
- **Platform**: GitHub (free for public and private repos)
- **Branch Strategy**:
  - `main` (production)
  - `develop` (staging / integration)
  - `feature/*` (feature branches)
  - `hotfix/*` (urgent production fixes)
- **Cost**: Free

### CI/CD
- **Platform**: GitHub Actions
- **Free Tier**: 2,000 minutes/month (private repos), unlimited for public repos
- **Workflows**:
  - PR checks: compile, test
  - Merge to `develop`: build Docker image, deploy to staging
  - Merge to `main`: deploy to production
- **Build**: `./mvnw clean package` → Docker image
- **Cost**: Free

### Hosting (Pilot Deployment)

| Component | Provider | Free Tier | Notes |
|---|---|---|---|
| **Backend** | **Render** | 750 hours/mo (spins down after inactivity) | ✅ Recommended — auto-deploys from GitHub |
| **Frontend** | **Vercel** | Unlimited for personal projects | ✅ Recommended — instant deploys, CDN |
| **Database** | **Neon** | 0.5 GB, auto-suspend | Free PostgreSQL in cloud |
| **Redis** | **Upstash** | 10K commands/day | Serverless, no credit card |
| **File Storage** | **Supabase Storage** | 1 GB | Or Cloudflare R2 (10 GB) |
| **DNS** | **Cloudflare** | Free DNS management | Free HTTPS, DDoS protection |
| **SSL** | **Let's Encrypt** | Free | Auto-renewed via hosting provider |

> **⚠️ Render Free Tier Note**: Backend spins down after 15 min of inactivity. First request after sleep takes ~30 seconds. This is acceptable for development and early pilot (clinicians use dashboard in bursts). Upgrade to $7/mo paid tier when hospital goes live.

### Monitoring & Logging
- **Health Checks**: Spring Actuator (`/actuator/health`, `/actuator/info`) — Free
- **Error Tracking**: Sentry (free tier: 5K errors/month) — https://sentry.io
- **Logging**: SLF4J + Logback → console output → Render logs (free)
- **Uptime Monitoring**: UptimeRobot (free: 50 monitors, 5-min checks) — https://uptimerobot.com
- **Cost**: Free

### Testing
- **Unit Tests**: JUnit 5 + Mockito — Free
- **Integration Tests**: Testcontainers (spins up PostgreSQL + Redis in Docker for tests) — Free
- **API Tests**: MockMvc (Spring) + REST Assured — Free
- **E2E (Frontend)**: Playwright 1.41 — Free
- **Coverage Target**: 70% (adjusted for solo developer)
- **Cost**: Free

---

## 5. Development Tools

### Code Quality
- **Linter/Formatter**: Spotless Maven Plugin (`spotless-maven-plugin`)
  - Google Java Style Guide
  - Run: `./mvnw spotless:apply`
- **Static Analysis**: SpotBugs (optional, for catching common bugs)
- **Git Hooks**: Pre-commit via Maven plugin (format + compile check)
- **Cost**: All free

### IDE Recommendation
- **Editor**: IntelliJ IDEA Community Edition — **free**
- **Plugins**:
  - Lombok (annotation processing)
  - Spring Boot Assistant (may need manual setup in CE)
  - Database Tools (built-in)
  - GitToolBox
- **Why IntelliJ CE**: Best-in-class Java support, refactoring, debugging — critical for learning Java
- **Important**: Community Edition is **100% free**. Do NOT buy Ultimate.
- **Download**: https://www.jetbrains.com/idea/download/ (select Community)

### VS Code (Frontend)
- **Extensions**:
  - ESLint — Free
  - Prettier — Free
  - Tailwind CSS IntelliSense — Free
  - TypeScript (built-in)
- **Cost**: Free

---

## 6. Environment Variables

### Required Variables

```bash
# Database (local Docker)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orthowatch
SPRING_DATASOURCE_USERNAME=orthowatch_user
SPRING_DATASOURCE_PASSWORD=orthowatch_pass

# Database (Neon — pilot)
# SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-host>/orthowatch?sslmode=require

# Redis (local Docker)
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Redis (Upstash — pilot)
# SPRING_DATA_REDIS_URL=rediss://default:<password>@<host>.upstash.io:6379

# JWT Auth
JWT_SECRET=<256-bit-random-string>
JWT_REFRESH_SECRET=<another-256-bit-random-string>
JWT_ACCESS_EXPIRY_MINUTES=30
JWT_REFRESH_EXPIRY_DAYS=7

# File Storage (local dev)
UPLOAD_DIR=./uploads

# File Storage (Supabase — pilot)
# SUPABASE_URL=https://<project>.supabase.co
# SUPABASE_ANON_KEY=...
# SUPABASE_BUCKET=wound-images

# WhatsApp Business API
WHATSAPP_API_URL=https://graph.facebook.com/v18.0
WHATSAPP_PHONE_NUMBER_ID=...
WHATSAPP_ACCESS_TOKEN=...
WHATSAPP_VERIFY_TOKEN=...

# Hospital Configuration
HOSPITAL_TIMEZONE=Asia/Kolkata
HOSPITAL_CHECKLIST_TIME=09:00
HOSPITAL_NAME=...

# App
APP_BASE_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:5173
SPRING_PROFILES_ACTIVE=dev

# Sentry (free tier)
SENTRY_DSN=...
```

---

## 7. Maven Commands

```xml
<!-- Key Maven commands (via Maven Wrapper) -->

# Development
./mvnw spring-boot:run              # Run app with hot-reload (DevTools)

# Build
./mvnw clean package                # Compile + test + package JAR
./mvnw clean package -DskipTests    # Package without running tests (faster)

# Testing
./mvnw test                         # Run all tests
./mvnw test -Dtest=RiskEngineTest   # Run specific test class

# Database
./mvnw flyway:migrate               # Run database migrations
./mvnw flyway:clean                 # Reset database (dev only!)

# Code Quality
./mvnw spotless:apply               # Auto-format code
./mvnw spotless:check               # Check formatting (used in CI)

# Dependency Management
./mvnw dependency:tree              # View dependency tree
./mvnw versions:display-dependency-updates  # Check for updates
```

### Frontend (package.json)
```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx",
    "format": "prettier --write ."
  }
}
```

---

## 8. Dependencies Lock

### Backend Dependencies (pom.xml)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.2</version>
</parent>

<properties>
    <java.version>21</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.1</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>10.6.0</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>

    <!-- FHIR -->
    <dependency>
        <groupId>ca.uhn.hapi.fhir</groupId>
        <artifactId>hapi-fhir-client</artifactId>
        <version>7.0.0</version>
    </dependency>
    <dependency>
        <groupId>ca.uhn.hapi.fhir</groupId>
        <artifactId>hapi-fhir-structures-r4</artifactId>
        <version>7.0.0</version>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>

    <!-- API Docs -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>

    <!-- Monitoring -->
    <dependency>
        <groupId>io.sentry</groupId>
        <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
        <version>7.3.0</version>
    </dependency>

    <!-- Dev Tools -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>5.4.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Frontend Dependencies (package.json)

```json
{
  "dependencies": {
    "react": "18.2.0",
    "react-dom": "18.2.0",
    "axios": "1.6.5",
    "@tanstack/react-query": "5.17.19",
    "zustand": "4.5.0",
    "react-hook-form": "7.49.3",
    "zod": "3.22.4",
    "@hookform/resolvers": "3.3.4",
    "recharts": "2.12.0",
    "lucide-react": "0.312.0",
    "react-router-dom": "6.22.0",
    "date-fns": "3.3.1",
    "clsx": "2.1.0",
    "tailwind-merge": "2.2.0"
  },
  "devDependencies": {
    "typescript": "5.3.3",
    "vite": "5.1.0",
    "@vitejs/plugin-react": "4.2.1",
    "tailwindcss": "3.4.1",
    "postcss": "8.4.33",
    "autoprefixer": "10.4.17",
    "eslint": "8.56.0",
    "prettier": "3.2.4",
    "@playwright/test": "1.41.1"
  }
}
```

---

## 9. Security Considerations

### Authentication Flow
1. Clinician submits email + password → `/api/auth/login`
2. Server validates credentials (BCrypt compare)
3. Returns JWT access token (30 min) + refresh token (7 days) in HTTP-only cookies
4. Frontend includes cookie automatically on every request
5. Spring Security filter validates JWT on each request
6. Role extracted from JWT claims → `@PreAuthorize` annotations enforce access

### Data Protection
- Passwords hashed with BCrypt (12 rounds)
- Patient data encrypted at rest (Neon encrypts by default, Supabase Storage encrypted)
- Data encrypted in transit (TLS 1.3 — enforced by hosting providers)
- SQL injection prevention (Spring Data JPA parameterized queries)
- XSS protection (React escaping + Spring Security headers)
- CORS strictly configured (frontend origin only)

### Rate Limiting
- Login attempts: 5 per 15 minutes per IP (Spring Security + custom filter)
- API requests: 100 per minute per authenticated user
- WhatsApp webhook: 500 per minute (Meta's rate)
- File uploads: 10 per hour per patient episode

### Audit Trail
- Every clinician action logged to `clinical_audit_log` table
- Fields: timestamp, user_id, action, risk_score_at_action, notes
- Spring Data Auditing annotations: `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`
- Immutable audit entries (no UPDATE/DELETE on audit tables)

### Compliance
- **DPDP Act 2023**: Consent captured before monitoring, stored with timestamp
- **NABH**: Full audit trail, role-based access, encrypted storage
- **Data Residency**: Neon/Supabase offer India-region hosting (free tier may be in Singapore — acceptable for pilot; migrate to AWS Mumbai post-pilot if required)

---

## 10. Version Upgrade Policy

### Major Version Updates
- Quarterly review of Spring Boot, Java, and PostgreSQL versions
- Test locally + in staging first
- Backwards compatibility check via integration tests
- Rollback plan: Docker image versioning with instant rollback

### Minor/Patch Updates
- Monthly security patches (prioritized)
- Maven dependency updates via `./mvnw versions:display-dependency-updates`
- Review and apply weekly

### Breaking Changes
- Document in CHANGELOG.md
- Update all related docs (PRD, APP_FLOW, TECH_STACK)
- Spring Boot major version upgrade: dedicated sprint

---

## 11. Project Structure

```
orthowatch/
├── backend/                          # Spring Boot application
│   ├── src/main/java/com/orthowatch/
│   │   ├── OrthoWatchApplication.java
│   │   ├── config/                   # Security, Redis, S3, Quartz configs
│   │   ├── controller/               # REST controllers
│   │   │   ├── AuthController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── PatientController.java
│   │   │   ├── EnrollmentController.java
│   │   │   └── AdminController.java
│   │   ├── service/                  # Business logic
│   │   │   ├── RiskEngineService.java
│   │   │   ├── WhatsAppService.java
│   │   │   ├── ChecklistService.java
│   │   │   ├── EnrollmentService.java
│   │   │   └── AuditService.java
│   │   ├── model/                    # JPA entities
│   │   │   ├── Patient.java
│   │   │   ├── Episode.java
│   │   │   ├── DailyResponse.java
│   │   │   ├── RiskScore.java
│   │   │   ├── Alert.java
│   │   │   ├── ConsentLog.java
│   │   │   └── ClinicalAuditLog.java
│   │   ├── repository/              # Spring Data JPA repositories
│   │   ├── dto/                     # Request/Response DTOs
│   │   ├── mapper/                  # MapStruct mappers
│   │   ├── scheduler/              # Quartz jobs
│   │   │   ├── ChecklistDispatchJob.java
│   │   │   ├── ReminderJob.java
│   │   │   └── EscalationJob.java
│   │   ├── rules/                   # YAML risk rule configs
│   │   └── exception/              # Custom exceptions + global handler
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   ├── db/migration/           # Flyway SQL migrations
│   │   │   ├── V1__initial_schema.sql
│   │   │   └── V2__seed_templates.sql
│   │   └── risk-rules.yml          # YAML risk rule definitions
│   ├── src/test/
│   ├── pom.xml                      # Maven build config
│   ├── mvnw                         # Maven Wrapper (Linux/Mac)
│   ├── mvnw.cmd                     # Maven Wrapper (Windows)
│   ├── Dockerfile
│   └── .mvn/wrapper/                # Maven Wrapper JAR
├── frontend/                        # React SPA
│   ├── src/
│   │   ├── components/             # shadcn/ui + custom components
│   │   ├── pages/                  # Dashboard, PatientDetail, Login, Admin
│   │   ├── hooks/                  # Custom React hooks
│   │   ├── services/               # Axios API client
│   │   ├── stores/                 # Zustand stores
│   │   ├── types/                  # TypeScript interfaces
│   │   └── App.tsx
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── tsconfig.json
├── docs/
│   ├── PRD.md
│   ├── APP_FLOW.md
│   └── TECH_STACK.md (this file)
├── docker-compose.yml               # PostgreSQL + Redis for local dev
├── .github/workflows/               # CI/CD
└── README.md
```

---

## 12. Local Development Quick Start

### Prerequisites (All Free)
1. **Java 21**: Download Eclipse Temurin from https://adoptium.net
2. **Node.js 20**: Download from https://nodejs.org
3. **Docker Desktop**: Download from https://docker.com (free for personal use)
4. **IntelliJ IDEA CE**: Download from https://jetbrains.com/idea/ (Community Edition)
5. **VS Code**: Download from https://code.visualstudio.com (for frontend)

### Run Locally

```bash
# 1. Start PostgreSQL + Redis via Docker
docker-compose up -d

# 2. Start backend (uses Maven Wrapper — no Maven install needed)
cd backend
./mvnw spring-boot:run          # Linux/Mac
mvnw.cmd spring-boot:run        # Windows

# 3. Start frontend (separate terminal)
cd frontend
npm install
npm run dev

# Backend:    http://localhost:8080
# Frontend:   http://localhost:5173
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator:   http://localhost:8080/actuator/health
```

### docker-compose.yml (for local dev)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: orthowatch
      POSTGRES_USER: orthowatch_user
      POSTGRES_PASSWORD: orthowatch_pass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  pgdata:
```
