# Product Requirements Document (PRD)

## 1. Product Overview

- **Project Title**: OrthoWatch – Post-Discharge Monitoring Pilot
- **Version**: 1.0
- **Last Updated**: Feb 2026
- **Owner**: [Founder / Product Lead Name]

---

## 2. Problem Statement

Orthopedic surgery patients (e.g., TKR, THR, ACL repair) are discharged within 3–5 days post-surgery, yet the highest risk of complications (infection, DVT, mobility delay) occurs within the first 7–14 days.

Hospitals currently rely on:
- Manual follow-up calls
- Scheduled OPD visits
- Patient-initiated complaints

This leads to:
- Delayed detection of complications
- Avoidable readmissions
- Bed occupancy inefficiency
- Inconsistent documentation
- High anxiety among patients

The hospital lacks a structured, scalable, protocol-driven remote recovery monitoring system.

---

## 3. Goals & Objectives

### Business Goals
1. Reduce 30-day readmission rate for orthopedic surgeries by ≥20% within 3 months of pilot.
2. Increase bed turnover efficiency in orthopedic unit by ≥10% within pilot duration.
3. Achieve ≥85% digital follow-up completion rate for Days 1–7.
4. Detect ≥70% of post-op complications before emergency presentation.

### User Goals

#### Surgeons & Clinicians
- Identify high-risk patients daily without reviewing all cases.
- Get structured recovery data instead of vague complaints.
- Detect complications early.

#### Patients
- Feel monitored and supported post-discharge.
- Easily report symptoms in vernacular.
- Avoid unnecessary hospital visits.

#### Caregivers
- Assist elderly patients in structured reporting.
- Know when escalation is needed.

---

## 4. Success Metrics

- **30-day readmission rate**: Reduced by ≥20% vs previous 3-month baseline.
- **Day 1–7 checklist completion rate**: ≥85%.
- **Flag-to-intervention time**: <24 hours for high-risk cases.
- **Patient satisfaction score**: ≥4/5.
- **Surgeon usability score**: ≥4/5.

**Measured via**:
- Hospital EMR data
- Dashboard analytics
- Post-pilot surveys

---

## 5. Target Users & Personas

### Primary Persona: Dr. Arjun Rao (Orthopedic Surgeon)
- **Demographics**: 42 years old, 12 years experience
- **Pain Points**:
  - No visibility once patient leaves hospital
  - Complications detected too late
  - Manual follow-ups inconsistent
- **Goals**:
  - Early risk detection
  - Structured patient data
  - Improve outcomes & reputation
- **Technical Proficiency**: Moderate (uses EMR daily)

### Secondary Persona: Lakshmi Devi (TKR Patient, 64)
- **Demographics**: Semi-urban, smartphone user
- **Pain Points**:
  - Anxiety after discharge
  - Unsure if pain/swelling is normal
  - Hesitant to travel for minor concerns
- **Goals**:
  - Clear guidance
  - Simple communication
  - Avoid complications
- **Technical Proficiency**: Low–Moderate (uses WhatsApp)

### Tertiary Persona: Caregiver (Son/Daughter)
- Assists elderly patient
- Responds to daily flows
- Needs clarity on escalation

---

## 6. Features & Requirements

### Must-Have Features (P0)

#### 1. Surgery-Specific Recovery Templates
- **Description**: System triggers structured Day 1–7 and Day 14 monitoring flows based on surgery type:
  - Total Knee Replacement (TKR)
  - Total Hip Replacement (THR)
  - ACL Repair
- **User Story**: As a surgeon, I want recovery monitoring to match the specific surgery type so that complication detection is clinically accurate.
- **Acceptance Criteria**:
  - System auto-selects correct template at discharge.
  - Templates include surgery-specific mobility milestones.
  - Day 14 check is automatically triggered.
  - Templates are editable by admin.
- **Success Metric**: ≥95% correct template assignment rate.

#### 2. WhatsApp-Based Structured Monitoring
- **Description**: Patients receive automated Day 1–7 structured checklists via WhatsApp.
- **Includes**:
  - Pain score (0–10)
  - Swelling level
  - Fever check
  - DVT check (calf pain, breathlessness)
  - Mobility milestone
  - Medication adherence
  - Wound image upload (mandatory Day 3 & 5)
  - Vernacular audio guidance
- **User Story**: As a patient, I want to report my recovery easily through WhatsApp so that I don’t need to travel unnecessarily.
- **Acceptance Criteria**:
  - Daily checklist sent at fixed time.
  - Structured response buttons (no free-text dependency).
  - Image upload supported.
  - Vernacular audio playback available.
  - Missed responses trigger reminder within 4 hours.
- **Success Metric**: ≥85% daily response rate.

#### 3. Rule-Based Risk Scoring Engine
- **Description**: System assigns risk score based on predefined rules.
- **Examples**:
  - Fever >100°F → High risk
  - Pain spike >2 points day-over-day → Medium risk
  - Increasing swelling trend → Medium risk
  - DVT symptoms → High risk
  - Wound redness + discharge → High risk
- **User Story**: As a clinician, I want automatic risk ranking so that I focus only on high-risk patients.
- **Acceptance Criteria**:
  - Risk score calculated daily.
  - Patients ranked by risk.
  - High-risk flagged in red.
  - Risk logic configurable by admin.
- **Success Metric**: ≥70% of complications first detected via system flags.

#### 4. Clinician Dashboard (Top 10 Risk View)
- **Description**: Dashboard shows:
  - Top 10 high-risk patients
  - Surgery type
  - Day post-op
  - Risk score
  - Pain/swelling trends
  - Wound image preview
  - Escalate / Call action
- **User Story**: As a surgeon, I want a prioritized patient list so that I don’t manually review everyone.
- **Acceptance Criteria**:
  - Dashboard loads within 3 seconds.
  - Top 10 risk patients clearly visible.
  - Trend graph visible for each patient.
  - Image preview expandable.
- **Success Metric**: Average daily review time <10 minutes per surgeon.

#### 5. Caregiver Mode
- **Description**: Allow secondary contact to respond on behalf of patient.
- **User Story**: As a caregiver, I want to respond for my elderly parent so that monitoring continues smoothly.
- **Acceptance Criteria**:
  - Secondary number can be added at discharge.
  - System logs whether response is patient or caregiver.
  - Caregiver receives same checklist.
- **Success Metric**: ≥30% elderly patients enrolled successfully via caregiver.

---

### Should-Have Features (P1)
- Surgeon performance analytics (complication trend per surgery type)
- Exportable recovery reports
- SMS fallback if WhatsApp fails

---

### Nice-to-Have (P2)
- ML-based complication prediction
- AI wound image analysis
- Integration with full hospital EMR

---

## 7. Explicitly OUT OF SCOPE

- ❌ Teleconsultation module
- ❌ Real-time video calls
- ❌ Multi-department rollout
- ❌ Long-term physiotherapy tracking (>30 days)
- ❌ Full EMR rebuild
- ❌ AI diagnosis
- ❌ Insurance claim processing
- ❌ Direct prescription management

---

## 8. User Scenarios

### Scenario 1: Infection Early Detection
- **Context**: Day 5 post TKR
- **Steps**:
  1. Patient reports fever 101°F.
  2. Uploads wound image showing redness.
  3. Risk engine flags high risk.
  4. Appears in Top 10 dashboard.
  5. Nurse calls patient within 6 hours.
- **Expected Outcome**: Early OPD intervention prevents readmission.
- **Edge Cases**:
  - Patient uploads blurred image
  - Patient skips fever question

### Scenario 2: DVT Suspicion
1. Patient reports calf pain + swelling.
2. Risk engine flags high priority.
3. Surgeon alerted.
4. Immediate evaluation scheduled.

### Scenario 3: Low-Risk Recovery
1. Patient reports stable pain decline.
2. Mobility milestones achieved.
3. Risk score remains low.
4. No intervention required.
- **Outcome**: Hospital resources preserved.

---

## 9. Dependencies & Constraints

### Technical Constraints
- Must use WhatsApp Business API
- Must comply with Indian data protection standards
- FHIR-compatible data ingestion

### Business Constraints
- Single hospital pilot
- Limited orthopedic unit
- 3 surgery types only
- 3-month pilot window

### External Dependencies
- WhatsApp API provider
- Hospital discharge data access
- IT approval

---

## 10. Timeline & Milestones

### Month 1
- Template design
- Risk rule design
- Dashboard wireframe

### Month 2
- WhatsApp integration
- Internal testing

### Month 3
- Pilot launch
- KPI tracking

---

## 11. Risks & Assumptions

### Risks
- Low patient engagement → **Mitigation**: vernacular audio
- Surgeon resistance → **Mitigation**: 10-min daily workflow design
- False positive flags → **Mitigation**: conservative rule calibration

### Assumptions
- Orthopedic unit performs ≥50 surgeries/month
- Hospital provides de-identified discharge summaries
- Patients use WhatsApp regularly

---

## 12. Non-Functional Requirements
- **Performance**: Dashboard load <3s
- **Security**: Encrypted data storage
- **Accessibility**: Large-button UI, audio prompts
- **Scalability**: Support 500 concurrent patients

---

## 13. References & Resources
- Orthopedic Post-Discharge Research Plan
- India Healthcare Business Model Study
- Hospital baseline readmission data