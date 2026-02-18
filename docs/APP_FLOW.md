# Application Flow Documentation

## OrthoWatch – Post-Discharge Monitoring System

**Last Updated**: Feb 2026
**Version**: 1.2 (Precision Upgrade)
**Based on**: OrthoWatch PRD v1.0
**Changelog v1.1**: Audit trail, escalation outcomes, consent capture, emergency override, enrollment validation, language persistence, KPI instrumentation, response locking, dashboard improvements, episode architecture, pilot-appropriate animations.
**Changelog v1.2**: Timezone handling, baseline validation, late response logic, image retention policy, risk rule versioning, escalation SLA, multi-surgeon assignment, checklist hard stop, dashboard refresh model, emergency follow-up UX, pilot scope boundaries.

---

## 1. Entry Points

### Primary Entry Points

- **Clinician Dashboard (Web)**
  - **Direct URL**: `https://app.orthowatch.in/dashboard`
  - User sees login page → redirected to Top 10 Risk Dashboard after auth
  - Accessible from hospital workstations and personal devices

- **Patient WhatsApp Flow**
  - **Trigger**: Automated daily message sent at 9:00 AM IST
  - Patient receives structured checklist via WhatsApp Business API
  - No app download required — native WhatsApp experience

- **Caregiver WhatsApp Flow**
  - **Trigger**: Same daily checklist forwarded to registered secondary contact
  - Entry via WhatsApp message link or direct reply

### Secondary Entry Points

- **Admin Portal**: `https://app.orthowatch.in/admin` — Template & rule management
- **Alert Notification Links**: Email/SMS alerts to clinicians with deep link to specific patient detail
- **Hospital EMR Discharge Event**: FHIR webhook triggers patient enrollment automatically

---

## 2. Core User Flows

### Flow 1: Patient Discharge & Enrollment

**Goal**: Register patient into monitoring system at time of discharge
**Entry Point**: Hospital EMR discharge event OR manual admin entry
**Frequency**: Per surgery (~50+/month)

#### Happy Path

1. **System Action**: Discharge event received via FHIR API
   - Patient demographics extracted (name, age, phone, surgery type, date)
   - Triggers template assignment
   - Creates new **Episode** under patient record (supports re-admissions without history mixing)

2. **System Action**: Auto-assign Recovery Template
   - Surgery type matched → TKR / THR / ACL template selected
   - Day 1–7 + Day 14 checklist schedule created
   - Elements: Template ID, checklist schedule, milestone definitions

3. **Page: Enrollment Validation Screen** *(Nurse — 2-minute workflow)*
   - **Route**: `/admin/enroll/:patient_id`
   - Nurse confirms before welcome message is sent:
     - ✅ Surgery type correct?
     - ✅ Phone number verified? (WhatsApp-enabled)
     - ✅ Caregiver added? (optional secondary number)
     - ✅ Preferred language selected? (English / Hindi / Tamil / etc.)
     - ✅ **Baseline snapshot recorded?** *(REQUIRED — enrollment cannot complete without this)*
       - `pain_score_discharge` (0–10)
       - `swelling_level_discharge` (None / Mild / Moderate / Severe)
     - ✅ **Primary surgeon assigned?**
       - `primary_surgeon_id` (required)
       - `secondary_clinician_id` (optional — for alert escalation fallback)
   - **Validation**:
     - Phone number format check
     - Surgery type must match template
     - **Baseline fields required** — form blocks submission if empty
     - Primary surgeon must be selected
   - **Action**: Click "Confirm & Send Welcome" → Triggers steps 4–6
   - **KPI Event**: `enrollment_confirmed`

4. **System Action**: Contact Registration
   - Primary contact: Patient WhatsApp number
   - Optional: Caregiver (secondary) number added by nurse
   - System logs contact type (patient / caregiver)
   - Language preference stored in patient profile → persists for all future messages

5. **System Action**: Digital Consent Capture (Day 0)
   - WhatsApp consent message sent before any monitoring begins:
     ```
     Welcome to OrthoWatch Recovery Monitoring.
     Before we begin, please confirm:
     ☐ I agree to participate in digital recovery monitoring
     ☐ I consent to sharing medical images for clinical review
     ☐ I consent to secure data storage of my health information
     
     Reply YES to confirm, or call [Hospital Number] for questions.
     ```
   - **Validation**: Patient must reply YES to proceed
   - **If no consent within 24 hours**: Nurse alerted to follow up manually
   - **Storage**: Consent timestamp + method stored in `consent_log` table
   - **KPI Event**: `consent_captured`
   - **Legal basis**: DPDP Act 2023 compliance, NABH audit trail

6. **System Action**: Welcome Message Sent *(only after consent)*
   - WhatsApp welcome message in **patient's selected language** with:
     - Surgery-specific recovery overview
     - What to expect (daily checklists)
     - Emergency contact info
     - Vernacular audio guidance attachment
   - Success State: Patient enrolled, consent recorded, Day 1 checklist scheduled
   - **KPI Event**: `enrollment_complete`

#### Error States

- **Invalid Phone Number**
  - Display: Admin receives validation error
  - Action: Nurse re-enters correct WhatsApp number

- **FHIR Webhook Failure**
  - Display: Alert in admin dashboard — "Manual enrollment required"
  - Action: Admin manually enters patient from discharge summary (CSV fallback)

- **Template Not Found for Surgery Type**
  - Display: Admin notification — "No template for [surgery type]"
  - Action: Default generic orthopedic template used, admin alerted to create specific template

#### Edge Cases

- Patient has no WhatsApp → Enroll caregiver as primary contact; SMS fallback (P1)
- Duplicate enrollment (re-admission) → System detects existing patient, creates **new episode** under same patient ID (episode-based architecture prevents historical data mixing)
- Discharge on weekend → Enrollment queued, first checklist sent next morning
- Patient declines consent → Enrollment paused, nurse notified, patient flagged as "Consent Pending"
- Nurse selects wrong language → Patient can change language via WhatsApp keyword "LANGUAGE" at any time

#### Exit Points

- Success: Patient enrolled, welcome message delivered
- Failure: Manual enrollment flagged in admin dashboard
- Abandonment: N/A (system-initiated flow)

---

### Flow 2: Daily Recovery Checklist (Patient/Caregiver)

**Goal**: Collect structured daily recovery data from patient
**Entry Point**: WhatsApp automated message at hospital-configured time
**Frequency**: Daily, Days 1–7 + Day 14

> **⏰ Timezone Rule**: All scheduled checklist triggers use **hospital-configured timezone** (default: IST / Asia/Kolkata). Timezone is stored at hospital level in system settings, not derived from server clock. This prevents production bugs if server is hosted in a different region.

#### Happy Path

1. **WhatsApp Message: Daily Checklist Dispatch**
   - Elements: Greeting, day number (e.g., "Day 3 Post-Surgery")
   - System sends structured interactive message
   - Trigger: Scheduled cron job at **hospital-configured time** (default: 9:00 AM in hospital timezone)

2. **WhatsApp Interaction: Pain Score**
   - Elements: Button selector (0–3 Low | 4–6 Moderate | 7–10 Severe)
   - User Action: Taps one button
   - Validation: Must select one option to proceed
   - System: Stores response, sends next question

3. **WhatsApp Interaction: Swelling Level**
   - Elements: Buttons (None | Mild | Moderate | Severe)
   - User Action: Taps one button

4. **WhatsApp Interaction: Fever Check**
   - Elements: Buttons (No Fever | Below 100°F | 100–102°F | Above 102°F)
   - User Action: Taps one button

5. **WhatsApp Interaction: DVT Screening**
   - Elements: Buttons (No Symptoms | Calf Pain | Swelling | Breathlessness)
   - User Action: Taps one or more
   - Audio: Vernacular audio explaining DVT symptoms plays before question
   - **🚨 EMERGENCY OVERRIDE**: If patient selects "Breathlessness":
     ```
     ⚠️ URGENT: Breathlessness after surgery may indicate a serious condition.
     
     👉 Please go to the nearest Emergency Department IMMEDIATELY.
     👉 Call Hospital Emergency: [Hospital Emergency Number]
     👉 Call 112 (National Emergency)
     
     Do NOT wait. Your safety is our priority.
     ```
     - Message sent **immediately** — does not wait for dashboard review
     - **KPI Event**: `emergency_override_triggered`
     - Alert simultaneously sent to assigned surgeon with HIGH priority
     - Remaining checklist questions skipped for this day
     - **Follow-up confirmation** (sent 5 minutes after override):
       ```
       Are you going to the hospital now?
       Reply YES to confirm.
       ```
     - If patient replies YES → Log `emergency_compliance_confirmed`
     - If no reply within 30 min → Nurse alerted for manual follow-up

6. **WhatsApp Interaction: Mobility Milestone**
   - Elements: Surgery-specific milestone checklist
     - TKR: "Can you bend knee to 90°?" (Yes/No)
     - THR: "Can you walk with walker for 10 minutes?" (Yes/No)
     - ACL: "Can you bear weight on operated leg?" (Yes/No)
   - User Action: Taps Yes/No

7. **WhatsApp Interaction: Medication Adherence**
   - Elements: Buttons (Took All | Missed Some | Didn't Take)
   - User Action: Taps one button

8. **WhatsApp Interaction: Wound Image Upload** *(Mandatory Day 3 & 5)*
   - Elements: "Please upload a clear photo of your surgical wound"
   - User Action: Takes photo and sends via WhatsApp
   - Validation: Image received confirmation
   - Audio: Vernacular guidance on how to take a clear wound photo

9. **System Action: Response Processing**
   - All responses stored in `daily_responses` table (linked to current **episode**)
   - Risk scoring engine triggered
   - Risk score calculated and stored
   - **Checklist Hard Stop Rule**: Checklist considered **complete** ONLY when:
     - All required structured responses submitted (pain, swelling, fever, DVT, mobility, medication)
     - Mandatory wound image received (if Day 3 or Day 5)
     - Incomplete checklists logged as `checklist_partial` (not `checklist_completed`)
     - This definition drives the ≥85% completion KPI calculation
   - **KPI Events**: `checklist_sent`, `checklist_completed` or `checklist_partial`
   - Success State: "Thank you! Your responses have been recorded. ✅"

#### Error States

- **No Response Within 4 Hours**
  - System: Sends reminder message — "We haven't heard from you today. Please complete your recovery checklist."
  - If caregiver enrolled: Forwards checklist to caregiver

- **No Response Within 8 Hours**
  - System: Escalation alert to nurse dashboard — "Patient [Name] non-responsive Day [X]"

- **Blurred/Unclear Wound Image**
  - System: "The image appears unclear. Please retake the photo in good lighting, close to the wound."
  - Allows re-upload

- **WhatsApp API Timeout**
  - System: Retry message delivery 3 times over 30 minutes
  - Fallback: SMS notification (P1) — "Please check WhatsApp for your daily checklist"

#### Edge Cases

- Patient responds partially (answers 3 of 7 questions) → Save partial data, send reminder for remaining
- Caregiver responds instead of patient → Log responder identity, accept responses normally
- Patient sends free-text instead of button → "Please use the buttons provided to respond"
- Patient sends image on non-mandatory day → Accept and store, flag as bonus data
- Patient completes checklist at 11 PM → Accept late responses, adjust risk score accordingly
- **Late response after escalation trigger**:
  ```
  IF patient responds AFTER 8-hour non-response escalation was triggered
  THEN:
  - Cancel non-response alert (if not yet reviewed by clinician)
  - Recalculate risk score with new data
  - Update dashboard immediately
  - Log KPI event: late_response_after_escalation
  - Keep escalation record for audit (do not delete)
  ```
  This prevents double-handling confusion between automated escalation and actual patient data.

#### Exit Points

- Success: All responses recorded, risk score updated
- Partial: Some responses missing, reminders queued
- Non-response: Escalation triggered after 8 hours

---

### Flow 3: Risk Engine Processing & Alert Generation

**Goal**: Calculate patient risk and alert clinicians for high-risk cases
**Entry Point**: Triggered after each daily response submission
**Frequency**: Daily per patient (batch) + on-demand per response

#### Happy Path

1. **System Action: Risk Score Calculation**
   - Inputs: Today's responses + historical trend data
   - Rules evaluated (YAML-configurable):
     ```
     IF fever_temp > 100°F
     THEN risk_level = HIGH, weight = 30
     
     IF pain_today - pain_yesterday > 2
     THEN risk_level = MEDIUM, weight = 15
     
     IF swelling = INCREASING over 2 days
     THEN risk_level = MEDIUM, weight = 15
     
     IF dvt_symptoms = ANY_PRESENT
     THEN risk_level = HIGH, weight = 30
     
     IF wound_image = REDNESS_OR_DISCHARGE
     THEN risk_level = HIGH, weight = 25
     ```
   - Composite risk score calculated (0–100)
   - **Risk Rule Versioning**: Each risk score calculation stores:
     - `rule_version_id` — Version identifier of the rule set used
     - `rule_set_snapshot` — JSON snapshot of all active rules at calculation time
     - **Purpose**: If a rule is changed mid-pilot, historical scores remain explainable. Audit-critical for understanding why a patient was flagged.

2. **System Action: Risk Classification**
   - 0–30: LOW → Green indicator, no alert
   - 31–60: MEDIUM → Yellow indicator, added to watch list
   - 61–100: HIGH → Red indicator, immediate alert

3. **System Action: Alert Generation** *(HIGH risk only)*
   - Alert created in `alerts` table
   - Push notification to assigned surgeon/nurse
   - Patient appears in Top 10 dashboard
   - **KPI Event**: `risk_flag_high`
   - Success State: Clinician notified within 15 minutes

4. **System Action: Recovery Trajectory Calculation**
   - Based on 3-day trend of pain, swelling, and mobility:
     - 🟢 **Improving**: Pain decreasing, mobility increasing
     - 🟡 **Stable**: No significant change
     - 🔴 **Worsening**: Pain increasing, mobility stalling or declining
   - Stored in patient record, displayed on dashboard cards

#### Error States

- **Insufficient Data for Scoring**
  - Display: Score marked as "Incomplete" on dashboard
  - Action: Partial score based on available data, flagged for manual review

- **Rule Engine Configuration Error**
  - Display: Admin alert — "Risk rule [rule_name] failed to evaluate"
  - Action: Default conservative scoring applied, admin notified to fix rule

#### Edge Cases

- Multiple HIGH triggers in same day → Highest composite score surfaces
- Patient on Day 1 (no historical data) → Score based on absolute values only, no trend
- Risk score drops dramatically day-over-day → Flag as "Rapid Improvement" (positive alert)

---

### Flow 4: Clinician Dashboard Review

**Goal**: Surgeon reviews high-risk patients and takes action
**Entry Point**: `https://app.orthowatch.in/dashboard` or alert notification deep link
**Frequency**: 1–2 times daily per surgeon

> **👨‍⚕️ Multi-Surgeon Assignment**: Each episode must have a `primary_surgeon_id` (required) and an optional `secondary_clinician_id`. All alerts route to primary surgeon first. If primary does not acknowledge alert within configurable threshold (default: 2 hours), alert auto-forwards to secondary clinician. This prevents missed cases during surgeon unavailability.

#### Happy Path

1. **Page: Login**
   - Elements: Email input, Password input, "Sign In" button
   - User Action: Enters credentials
   - Validation: Email format, password required
   - Trigger: JWT token issued, redirect to dashboard

2. **Page: Top 10 Risk Dashboard**
   - Elements:
     - Risk-sorted patient cards (Top 10)
     - For each card:
       - Patient name, surgery type, day post-op
       - Risk score badge (red/yellow/green)
       - **"New Today" badge** — if patient appeared in Top 10 since last surgeon visit
       - **Recovery trajectory indicator**: Improving 🟢 / Stable 🟡 / Worsening 🔴
       - Trend sparkline
     - Quick actions: "View Details", "Call", "Escalate"
     - **"Non-Responsive Today" mini-section** — separate grouped list of patients who haven't responded within 8 hours (prevents burial in main list)
   - User Action: Clicks on patient card
   - Trigger: Navigate to Patient Detail page

3. **Page: Patient Detail**
   - Elements:
     - Patient demographics (name, age, surgery, discharge date, **current episode ID**)
     - **Baseline Snapshot** (captured at discharge):
       - Baseline pain score
       - Baseline swelling level
       - So surgeon sees improvement vs discharge state
     - Day-by-day response timeline
     - Pain trend graph (line chart, 7-day) with **baseline reference line**
     - Swelling trend graph with **baseline reference line**
     - Wound image gallery (expandable thumbnails)
     - Risk score history graph
     - Recovery trajectory badge (Improving / Stable / Worsening)
     - Contributing risk factors (highlighted rules triggered)
   - User Actions:
     - Expand wound image → Full-screen modal
     - Click "Call Patient" → Opens phone dialer / logs call intent
     - Click "Escalate" → **Escalation modal with outcome selection** (see below)
     - Click "Mark Reviewed" → Clears from Top 10, logs review timestamp
   - **📝 Audit Trail**: Every clinician action creates an audit log entry:
     - `timestamp` — When action was taken
     - `user_id` — Which clinician
     - `action` — Call / Escalate / Mark Reviewed / View Image
     - `risk_score_at_action` — Patient's risk score at time of action
     - `notes` — Optional clinician notes
     - Stored in `clinical_audit_log` table
     - **Purpose**: NABH compliance, legal defensibility, surgeon protection
   - **KPI Event**: `escalation_triggered` (on escalate), `patient_reviewed` (on mark reviewed)
   - Success State: Patient reviewed, action taken or marked safe

   **Escalation Outcome Capture** *(required when escalating)*:
   - When clinician clicks "Escalate", modal requires selection of planned action:
     - ○ OPD visit scheduled
     - ○ Telephonic advice given
     - ○ Medication adjusted
     - ○ ER referral advised
     - ○ No action needed (false positive)
   - Stored as `escalation_outcome` in `alerts` table
   - **KPI Event**: `escalation_resolved` (when outcome selected)
   - **Purpose**: KPI measurement, readmission prevention analytics, surgeon reporting

4. **Page: All Patients List** *(Secondary view)*
   - Elements: Searchable/filterable table of all active patients
   - Filters: Surgery type, day post-op, risk level, response status
   - User Action: Click row → Patient Detail page

#### Error States

- **Dashboard Load Timeout (>3s)**
  - Display: Skeleton loading cards
  - Action: Retry API call, show cached data if available

- **Session Expired**
  - Display: "Session expired" modal
  - Action: Redirect to login, preserve intended destination

#### Edge Cases

- No high-risk patients today → Dashboard shows "All patients are recovering well ✅" with link to full list
- Surgeon reviews patient, then new data arrives → Real-time update or "Refresh" indicator
- Multiple surgeons viewing same patient → Last action wins, audit log preserved

#### Exit Points

- Success: All high-risk patients reviewed, actions logged
- Partial: Some patients reviewed, others remain in Top 10
- Redirect: Logout → Login page

---

### Flow 5: Admin Template & Rule Management

**Goal**: Admin configures recovery templates and risk scoring rules
**Entry Point**: `https://app.orthowatch.in/admin`
**Frequency**: As needed (setup phase + periodic updates)

#### Happy Path

1. **Page: Admin Login**
   - Same as clinician login with admin role verification

2. **Page: Template Management**
   - Elements: List of surgery templates (TKR, THR, ACL)
   - User Actions:
     - Edit template → Opens template editor
     - Create new template → Blank template form
   - Template Editor Elements:
     - Surgery type selector
     - Day-by-day checklist builder (drag-and-drop questions)
     - Milestone definitions per day
     - Mandatory image upload days selector
   - Trigger: Save template → Active for new enrollments

3. **Page: Risk Rule Configuration**
   - Elements: List of active rules with conditions and weights
   - User Actions:
     - Edit rule → Inline editor (condition, risk_level, weight)
     - Add new rule → Rule builder form
     - Toggle rule active/inactive
   - Validation: Rule syntax check before save
   - Trigger: Save → Rules effective for next risk calculation

#### Error States

- **Invalid Rule Syntax**
  - Display: Inline validation error — "Condition format invalid"
  - Action: Highlight error, prevent save

- **Template Save Conflict**
  - Display: "Another admin modified this template"
  - Action: Show diff, allow merge or overwrite

---

## 3. Navigation Map

### Clinician Dashboard Navigation

```
Dashboard (app.orthowatch.in)
├── Login
├── Top 10 Risk Dashboard [AUTH: Surgeon/Nurse]
│   ├── "New Today" badges
│   ├── "Non-Responsive Today" section
│   └── Patient Detail
│       ├── Baseline Snapshot
│       ├── Response Timeline
│       ├── Trend Graphs (with baseline reference)
│       ├── Recovery Trajectory (Improving/Stable/Worsening)
│       ├── Wound Image Gallery
│       ├── Clinical Audit Log
│       └── Actions (Call / Escalate+Outcome / Mark Reviewed)
├── All Patients [AUTH: Surgeon/Nurse]
│   └── Patient Detail (same as above)
├── Analytics [AUTH: Surgeon/Admin] (P1)
│   ├── Completion Rates
│   ├── Complication Trends
│   └── Surgeon Performance
└── Admin Panel [AUTH: Admin]
    ├── Enrollment Validation [AUTH: Nurse/Admin]
    ├── Template Management
    │   └── Template Editor
    ├── Risk Rule Configuration
    │   └── Rule Editor
    ├── User Management
    └── System Settings
```

### WhatsApp Flow Navigation

```
WhatsApp (Patient/Caregiver)
├── Digital Consent Capture (Day 0)
├── Welcome Message (after consent)
├── Daily Checklist (Days 1–7, 14)
│   ├── Pain Score → Swelling → Fever → DVT → Mobility → Medication → [Image]
│   ├── 🚨 Emergency Override (on breathlessness) → Immediate ER guidance
│   └── Completion Confirmation
├── Reminders (4-hour / 8-hour)
├── Language Change (keyword: "LANGUAGE")
└── Emergency Guidance (on-demand keyword trigger)
```

### Navigation Rules

- **Authentication Required**: All dashboard pages (Top 10, Patient Detail, All Patients, Analytics, Admin)
- **Role-Based Access**:
  - Surgeon: Dashboard, Patient Detail, Analytics (own patients)
  - Nurse: Dashboard, Patient Detail, Call/Escalate actions
  - Admin: All pages + Template/Rule management + User management
- **Redirect Logic**: Unauthenticated users → Login page → Original destination after auth
- **Back Button Behavior**: Patient Detail → Return to Dashboard/All Patients (preserves scroll position)

---

## 4. Screen Inventory

### Screen: Login

- **Route**: `/login`
- **Access**: Public
- **Purpose**: Authenticate clinicians and admins
- **Key Elements**: Email input, password input, "Sign In" button, "Forgot Password" link
- **Actions Available**:
  - Submit credentials → Dashboard (success) or Error message (failure)
  - Forgot password → Password reset flow
- **State Variants**: Default, Loading (submitting), Error (invalid credentials)

### Screen: Top 10 Risk Dashboard

- **Route**: `/dashboard`
- **Access**: Authenticated (Surgeon, Nurse)
- **Purpose**: Surface highest-risk patients requiring immediate attention
- **Key Elements**:
  - Risk-sorted patient cards (max 10)
  - Each card: Name, surgery type, day post-op, risk badge, recovery trajectory indicator, trend sparkline
  - **"New Today" badge** on patients first appearing since last visit
  - **"Non-Responsive Today" mini-section** (separate grouped list)
  - Quick action buttons per card
  - "View All Patients" link
  - Last refreshed timestamp
- **Actions Available**:
  - Click patient card → `/patients/:id`
  - Click "Call" → Phone dialer + audit log
  - Click "Escalate" → Escalation modal with outcome selection
  - Click "View All" → `/patients`
- **State Variants**: Loading (skeleton cards), Empty ("All patients recovering well"), Populated, Error (API failure)

### Screen: Patient Detail

- **Route**: `/patients/:id`
- **Access**: Authenticated (Surgeon, Nurse)
- **Purpose**: Deep-dive into individual patient recovery data
- **Key Elements**:
  - Patient header (demographics, surgery info, **episode ID**)
  - **Baseline Snapshot** (pain at discharge, swelling at discharge)
  - Response timeline (day-by-day)
  - Pain/swelling trend charts **with baseline reference lines**
  - Wound image gallery (expandable)
  - Risk score history chart
  - **Recovery trajectory badge** (Improving 🟢 / Stable 🟡 / Worsening 🔴)
  - **Clinical audit log** (all actions with timestamps)
  - Action buttons (Call, Escalate + Outcome, Mark Reviewed)
- **Actions Available**:
  - Expand image → Full-screen modal
  - Call → Phone dialer / audit log entry
  - Escalate → Outcome selection modal → Alert + audit log
  - Mark Reviewed → Patient removed from Top 10, audit logged
  - Back → Dashboard or All Patients
- **State Variants**: Loading, Populated, Error, No Data Yet (recently enrolled)

### Screen: All Patients

- **Route**: `/patients`
- **Access**: Authenticated (Surgeon, Nurse)
- **Purpose**: Browse and search all active patients
- **Key Elements**: Searchable table, filters (surgery type, risk level, day post-op, response status), sortable columns
- **Actions Available**:
  - Click row → `/patients/:id`
  - Apply filters → Filter results
  - Export (P1) → Download CSV
- **State Variants**: Loading, Populated, Empty (no patients), Filtered (subset)

### Screen: Admin — Template Management

- **Route**: `/admin/templates`
- **Access**: Authenticated (Admin)
- **Purpose**: Create and edit surgery-specific recovery templates
- **Key Elements**: Template list, "Create New" button, edit/delete actions per template
- **Actions Available**:
  - Click template → `/admin/templates/:id/edit`
  - Create New → `/admin/templates/new`
- **State Variants**: Loading, Populated, Empty

### Screen: Admin — Risk Rule Configuration

- **Route**: `/admin/rules`
- **Access**: Authenticated (Admin)
- **Purpose**: Configure risk scoring rules and weights
- **Key Elements**: Rule list with condition preview, weight, status (active/inactive)
- **Actions Available**:
  - Edit rule → Inline editor or modal
  - Add rule → Rule builder form
  - Toggle active/inactive → Immediate effect
- **State Variants**: Loading, Populated, Empty

### Screen: Enrollment Validation

- **Route**: `/admin/enroll/:patient_id`
- **Access**: Authenticated (Nurse, Admin)
- **Purpose**: Nurse validates patient data before welcome message dispatch
- **Key Elements**: Surgery type confirmation, phone number verification, caregiver toggle, language selector, baseline pain/swelling inputs
- **Actions Available**:
  - Confirm & Send Welcome → Triggers consent message + enrollment
  - Cancel → Returns to admin panel
- **State Variants**: Default, Loading (sending), Success (enrolled), Error (validation failed)

---

## 5. Interaction Patterns

### Pattern: WhatsApp Structured Response

- Trigger: Patient receives interactive message with buttons
- Interaction: Tap one button option
- Validation: Single selection per question (enforced by WhatsApp API)
- Success: Next question sent immediately
- Error: Invalid response → "Please use the buttons to respond"

### Pattern: Wound Image Upload

- Trigger: System requests wound photo (Day 3 & 5)
- Interaction: Patient takes photo, sends via WhatsApp attachment
- Validation: File type check (image/jpeg, image/png), size < 10MB
- Success: "Image received ✅" confirmation
- Error: "Please send a photo (not a document or video)"

### Pattern: Dashboard Data Loading

- Trigger: Page load or refresh
- Loading State: Skeleton cards / shimmer effect
- Success: Data populated, cards rendered
- Error: "Unable to load patient data. Retry?" button
- Stale Data: Show timestamp — "Last updated 5 min ago" with refresh button
- **Refresh Model**:
  - Auto-refresh: Every **5 minutes** (configurable in admin settings)
  - Manual refresh: Button always visible in dashboard header
  - Real-time WebSocket updates: **Optional** (if infra supports; not required for pilot)
  - On refresh: Only changed patient cards re-render (not full page reload)

### Pattern: Escalation Action

- Trigger: Clinician clicks "Escalate" on patient card
- Step 1: Modal — "Escalate [Patient Name] to senior surgeon?"
- Step 2: **Required outcome selection**:
  - ○ OPD visit scheduled
  - ○ Telephonic advice given
  - ○ Medication adjusted
  - ○ ER referral advised
  - ○ No action needed (false positive)
- Loading: Button disabled, spinner
- Success: "Escalation alert sent ✅" toast notification
- Error: "Failed to send escalation. Please try again." toast
- **Audit**: Creates `clinical_audit_log` entry with timestamp, user, action, risk score, and outcome
- **KPI Events**: `escalation_triggered`, `escalation_resolved`

### Pattern: Form Submission (Admin)

- Validation: Client-side (instant field validation) + server-side (on save)
- Loading State: Disabled save button, spinner
- Success: "Template saved successfully" toast, redirect to list
- Error: Inline field errors, form data preserved

---

## 6. Decision Points

### Decision: Patient Response Status

```
IF patient has responded today
THEN show: Green checkmark on patient card
AND update: Risk score with new data

ELSE IF patient has NOT responded within 4 hours
THEN send: Reminder via WhatsApp
AND show: Yellow "Pending" badge on dashboard

ELSE IF patient has NOT responded within 8 hours
THEN send: Escalation to nurse
AND show: Orange "Non-Responsive" badge on dashboard
AND trigger: Forward checklist to caregiver (if enrolled)
```

### Decision: Risk Level Classification

```
IF composite_risk_score > 60
THEN classify: HIGH
AND show: Red badge
AND add to: Top 10 dashboard
AND send: Alert to assigned clinician

ELSE IF composite_risk_score > 30
THEN classify: MEDIUM
AND show: Yellow badge
AND add to: Watch list

ELSE
THEN classify: LOW
AND show: Green badge
AND action: No alert needed
```

### Decision: Wound Image Required

```
IF current_day IN [3, 5]
THEN require: Wound image upload (mandatory)
AND show: "Please upload wound photo" prompt
AND block: Checklist completion until image received

ELSE IF current_day NOT IN [3, 5]
THEN allow: Optional image upload
AND skip: Image prompt in checklist flow
```

### Decision: Caregiver Fallback

```
IF patient has caregiver enrolled
AND patient has NOT responded within 4 hours
THEN forward: Checklist to caregiver
AND log: "Forwarded to caregiver" in response record

ELSE IF patient has NO caregiver enrolled
AND patient has NOT responded within 4 hours
THEN send: Reminder to patient only
AND flag: "No caregiver backup" in dashboard
```

### Decision: User Role Access

```
IF user.role = ADMIN
THEN access: All pages including Admin panel
AND allow: Template/Rule edit, User management

ELSE IF user.role = SURGEON
THEN access: Dashboard, Patient Detail, Analytics (own patients)
AND deny: Admin panel

ELSE IF user.role = NURSE
THEN access: Dashboard, Patient Detail, Enrollment Validation
AND allow: Call, Escalate actions
AND deny: Admin panel, Analytics
```

### Decision: Emergency Auto-Override

```
IF dvt_response INCLUDES "Breathlessness"
OR dvt_response INCLUDES "Chest Pain"
THEN send: IMMEDIATE emergency message to patient
AND skip: Remaining checklist questions
AND flag: CRITICAL alert to surgeon (bypass normal queue)
AND log: KPI event emergency_override_triggered
AND action: Do NOT wait for dashboard review
```

### Decision: Response Locking After Escalation

```
IF patient.escalation_status = ACTIVE
AND escalation was triggered TODAY
THEN pause: Further automated reminders/nudges for that day
AND show: "Escalation Active" badge on patient card
AND skip: Non-response alerts (already being handled)

ELSE IF patient.escalation_status = RESOLVED
THEN resume: Normal checklist schedule next day
```

---

## 7. Error Handling Flows

### 404 Not Found

- **Display**: Custom 404 page — "Patient or page not found"
- **Actions**: "Go to Dashboard" button, Search patients bar
- **Log**: 404 errors tracked for broken links/bookmarks

### 500 Server Error

- **Display**: Friendly error page — "Something went wrong. Our team has been notified."
- **Actions**: "Retry" button, "Contact IT Support" link
- **Fallback**: Show cached dashboard data if available

### Network Offline (Dashboard)

- **Display**: Top banner — "You are offline. Showing last loaded data."
- **Actions**: Auto-retry connection every 30 seconds
- **Recovery**: Refresh data automatically when online

### WhatsApp API Failure

- **Display**: Admin dashboard alert — "WhatsApp delivery failed for [X] patients"
- **Actions**: Retry delivery button, SMS fallback option (P1)
- **Recovery**: Auto-retry 3 times over 30 minutes, then mark as failed

### Permission Denied

- **Display**: "You don't have permission to access this page"
- **Actions**: "Go to Dashboard" button, "Request Access" link
- **Log**: Unauthorized access attempts logged for audit

---

## 8. Responsive Behavior

### Mobile-Specific Flows (Clinician on phone)

- **Navigation**: Bottom tab bar (Dashboard | Patients | Profile)
- **Patient Cards**: Full-width stacked cards, swipe for actions
- **Patient Detail**: Vertical scroll, collapsible sections
- **Charts**: Horizontal scroll for trend graphs
- **Image Gallery**: Full-screen swipe gallery
- **Actions**: Bottom sheet for Call/Escalate/Review options

### Tablet-Specific Flows

- **Navigation**: Side drawer (collapsible)
- **Dashboard**: 2-column card grid
- **Patient Detail**: Split view (info left, charts right)

### Desktop-Specific Flows

- **Navigation**: Fixed left sidebar with full labels
- **Dashboard**: 3-column card grid with detail panel on right
- **Patient Detail**: Multi-column layout (demographics + timeline left, charts + images right)
- **Admin**: Full-width table views with inline editing
- **Modals**: Centered modals for confirmations and escalations

---

## 9. Animation & Transitions

> **⚕️ Hospital Pilot Note**: Animations are kept **minimal and functional** for clinical environments. Hospital users prioritize speed, clarity, and reliability over visual flair. All animations are optional and can be disabled via admin settings.

### Page Transitions

- **Navigation between pages**: Fade in/out (150ms, ease-in-out) — fast, non-distracting
- **Modal open**: Fade in (150ms)
- **Modal close**: Fade out (100ms)

### Functional Micro-interactions

- **Risk Badge**: Static color badges (no pulse animation — reduces distraction during clinical review)
- **Toast Notification**: Slide in from top-right (200ms), auto-dismiss after 3s
- **Button Click**: Subtle opacity change on press (50ms)

### Loading States

- **Dashboard**: Skeleton card placeholders (no shimmer — fast load target <3s makes this minimal)
- **Patient Detail**: Progressive section load
- **Charts**: Spinner → instant data render
- **Images**: Low-res placeholder → full resolution

### Success Feedback

- **Checklist Complete** (WhatsApp): ✅ emoji + "All responses recorded"
- **Mark Reviewed** (Dashboard): Card removed from Top 10 (instant, no slide animation)
- **Escalation Sent**: "Escalation sent ✅" toast notification

---

## 10. KPI Instrumentation Events

All events below are tracked for analytics, dashboard KPIs, and pilot evaluation.

| Event Name | Trigger | Purpose |
|---|---|---|
| `enrollment_confirmed` | Nurse confirms enrollment | Track enrollment workflow completion |
| `consent_captured` | Patient replies YES to consent | Legal compliance tracking |
| `enrollment_complete` | Welcome message delivered | End-to-end enrollment success |
| `checklist_sent` | Daily checklist dispatched | Delivery rate tracking |
| `checklist_completed` | All questions answered | Day 1–7 completion rate (target: ≥85%) |
| `checklist_partial` | Some questions answered | Drop-off analysis |
| `non_response_4hr` | No response within 4 hours | Engagement tracking |
| `non_response_8hr` | No response within 8 hours | Escalation trigger tracking |
| `risk_flag_high` | Risk score > 60 | Complication detection rate (target: ≥70%) |
| `emergency_override_triggered` | Breathlessness/critical symptom reported | Emergency pathway usage |
| `escalation_triggered` | Clinician escalates patient | Flag-to-intervention time tracking |
| `escalation_resolved` | Clinician selects escalation outcome | Outcome capture for analytics |
| `patient_reviewed` | Clinician marks patient as reviewed | Surgeon engagement tracking |
| `late_response_after_escalation` | Patient responds after 8hr escalation trigger | Double-handling prevention tracking |
| `emergency_compliance_confirmed` | Patient replies YES to emergency follow-up | Emergency pathway compliance |
| `readmission_within_30d` | Patient readmitted within 30 days | Primary KPI — target: ≥20% reduction |

### Escalation SLA Tracking

System automatically calculates:

```
escalation_response_time = escalation_resolved_timestamp - escalation_triggered_timestamp
```

- Exposed in **Analytics dashboard** (even if P1, define now for data collection)
- Directly ties to PRD metric: **Flag-to-intervention < 24 hours**
- SLA breach alert: If `escalation_response_time > 24 hours`, flag in admin dashboard

### Measurement Alignment with PRD

| PRD Success Metric | Tracked By |
|---|---|
| 30-day readmission rate ≥20% reduction | `readmission_within_30d` vs baseline |
| Day 1–7 checklist completion ≥85% | `checklist_completed` / `checklist_sent` |
| Flag-to-intervention <24 hours | `risk_flag_high` → `escalation_triggered` time delta |
| Patient satisfaction ≥4/5 | Post-pilot survey (external) |
| Surgeon usability ≥4/5 | Post-pilot survey (external) |

---

## 11. Data Architecture Note: Episode-Based Model

To support re-admissions without historical data mixing:

```
Patient (patient_id)
└── Episode (episode_id, surgery_type, discharge_date, primary_surgeon_id, secondary_clinician_id)
    ├── Consent Log (consent_timestamp, method)
    ├── Baseline Snapshot (pain_score_discharge, swelling_level_discharge)
    ├── Daily Responses (day, pain, swelling, fever, dvt, mobility, medication, image_url)
    ├── Risk Scores (date, score, contributing_factors, trajectory, rule_version_id, rule_set_snapshot)
    ├── Alerts (alert_type, status, escalation_outcome, escalation_response_time)
    └── Clinical Audit Log (timestamp, user_id, action, risk_score_at_action, notes)
```

Each re-admission creates a **new Episode** under the same Patient. All daily responses, risk scores, alerts, and audit logs are scoped to the active episode. Historical episodes remain accessible for longitudinal analysis but do not affect current scoring.

### Image Storage & Retention Policy

- Wound images retained for **minimum 3 years** (hospital audit requirement)
- After retention period: Images archived or deleted per hospital data governance policy
- Storage: Encrypted at rest (AES-256), access logged in audit trail
- **Hospitals will ask about this during onboarding** — retention period configurable per hospital

---

## 12. Pilot Scope Boundaries

### In Scope (Pilot v1)

- **Single hospital** deployment
- **Single orthopedic unit** (one department)
- **Surgery types**: TKR (Total Knee Replacement), THR (Total Hip Replacement), ACL Reconstruction
- **Monitoring window**: 7-day intensive + Day 14 follow-up
- **Max concurrent active episodes**: 150
- **Timezone**: Single hospital timezone (default IST)
- **Languages**: English + 1 regional language (configurable)

### Out of Scope (Pilot v1)

- Cross-hospital rollout / multi-tenant architecture
- Real-time teleconsult / video calling
- AI-powered diagnosis or image analysis
- Multi-department expansion (cardiology, general surgery, etc.)
- Patient mobile app (WhatsApp-only for pilot)
- EMR write-back (read-only FHIR integration for pilot)
- Cross-state timezone handling (single hospital = single timezone)
