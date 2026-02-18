-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users (Clinicians/Admins)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'SURGEON', 'NURSE')),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- 2. Patients
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(255) NOT NULL,
    age INTEGER NOT NULL CHECK (age > 0 AND age < 150),
    gender VARCHAR(10),
    phone_primary VARCHAR(20) NOT NULL,
    phone_caregiver VARCHAR(20),
    preferred_language VARCHAR(10) DEFAULT 'en' NOT NULL,
    hospital_mrn VARCHAR(100),
    fhir_patient_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_patients_phone_primary ON patients(phone_primary);
CREATE INDEX idx_patients_hospital_mrn ON patients(hospital_mrn);

-- 3. Recovery Templates
CREATE TABLE recovery_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    surgery_type VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    checklist_config JSONB NOT NULL,
    milestone_config JSONB NOT NULL,
    mandatory_image_days INTEGER[] DEFAULT '{3,5}' NOT NULL,
    monitoring_days INTEGER DEFAULT 14 NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_templates_is_active ON recovery_templates(is_active);

-- 4. Episodes
CREATE TABLE episodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES recovery_templates(id),
    primary_surgeon_id UUID NOT NULL REFERENCES users(id),
    secondary_clinician_id UUID REFERENCES users(id),
    surgery_date DATE NOT NULL,
    discharge_date DATE NOT NULL,
    current_day INTEGER DEFAULT 0 NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED')),
    pain_score_discharge INTEGER NOT NULL CHECK (pain_score_discharge >= 0 AND pain_score_discharge <= 10),
    swelling_level_discharge VARCHAR(20) NOT NULL CHECK (swelling_level_discharge IN ('NONE', 'MILD', 'MODERATE', 'SEVERE')),
    consent_status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    consent_timestamp TIMESTAMP WITH TIME ZONE,
    checklist_time TIME DEFAULT '09:00' NOT NULL,
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_episodes_patient_id ON episodes(patient_id);
CREATE INDEX idx_episodes_primary_surgeon_id ON episodes(primary_surgeon_id);
CREATE INDEX idx_episodes_status ON episodes(status);

-- 5. Daily Responses
CREATE TABLE daily_responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL CHECK (day_number >= 1 AND day_number <= 30),
    responder_type VARCHAR(20) NOT NULL CHECK (responder_type IN ('PATIENT', 'CAREGIVER')),
    pain_score INTEGER CHECK (pain_score >= 0 AND pain_score <= 10),
    swelling_level VARCHAR(20) CHECK (swelling_level IN ('NONE', 'MILD', 'MODERATE', 'SEVERE')),
    fever_level VARCHAR(20) CHECK (fever_level IN ('NO_FEVER', 'BELOW_100', '100_TO_102', 'ABOVE_102')),
    dvt_symptoms VARCHAR(100)[],
    mobility_achieved BOOLEAN,
    medication_adherence VARCHAR(20) CHECK (medication_adherence IN ('TOOK_ALL', 'MISSED_SOME', 'DIDNT_TAKE')),
    completion_status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    emergency_override BOOLEAN DEFAULT FALSE NOT NULL,
    response_started_at TIMESTAMP WITH TIME ZONE,
    response_completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    UNIQUE(episode_id, day_number)
);
CREATE INDEX idx_daily_responses_episode_id ON daily_responses(episode_id);

-- 6. Risk Scores
CREATE TABLE risk_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL,
    composite_score INTEGER NOT NULL CHECK (composite_score >= 0 AND composite_score <= 100),
    risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    contributing_factors JSONB NOT NULL,
    trajectory VARCHAR(20) CHECK (trajectory IN ('IMPROVING', 'STABLE', 'WORSENING')),
    rule_version_id VARCHAR(50) NOT NULL,
    rule_set_snapshot JSONB NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_risk_scores_episode_day ON risk_scores(episode_id, day_number);

-- 7. Alerts
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    risk_score_id UUID REFERENCES risk_scores(id),
    alert_type VARCHAR(30) NOT NULL CHECK (alert_type IN ('HIGH_RISK', 'NON_RESPONSE', 'EMERGENCY_OVERRIDE', 'CONSENT_TIMEOUT')),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    assigned_to UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'RESOLVED', 'EXPIRED', 'CANCELLED')),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    escalation_outcome VARCHAR(50) CHECK (escalation_outcome IN ('OPD_SCHEDULED', 'TELEPHONIC_ADVICE', 'MEDICATION_ADJUSTED', 'ER_REFERRAL', 'FALSE_POSITIVE')),
    escalation_notes TEXT,
    auto_forwarded BOOLEAN DEFAULT FALSE NOT NULL,
    sla_deadline TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_assigned_to ON alerts(assigned_to);

-- 8. Wound Images
CREATE TABLE wound_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL,
    storage_path TEXT NOT NULL,
    storage_provider VARCHAR(20) NOT NULL CHECK (storage_provider IN ('LOCAL', 'SUPABASE', 'CLOUDFLARE_R2')),
    file_size_bytes BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    is_mandatory BOOLEAN NOT NULL,
    encrypted BOOLEAN DEFAULT TRUE NOT NULL,
    retention_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_by VARCHAR(20) NOT NULL CHECK (uploaded_by IN ('PATIENT', 'CAREGIVER')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_wound_images_episode_day ON wound_images(episode_id, day_number);

-- 9. Consent Logs
CREATE TABLE consent_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id),
    consent_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('GRANTED', 'DECLINED', 'REVOKED')),
    method VARCHAR(20) NOT NULL CHECK (method IN ('WHATSAPP', 'MANUAL', 'VERBAL')),
    consent_text TEXT NOT NULL,
    ip_address VARCHAR(45),
    granted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_consent_logs_episode_id ON consent_logs(episode_id);

-- 10. Clinical Audit Log
CREATE TABLE clinical_audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    episode_id UUID REFERENCES episodes(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id UUID,
    risk_score_at_action INTEGER,
    details JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_audit_user_id ON clinical_audit_log(user_id);
CREATE INDEX idx_audit_created_at ON clinical_audit_log(created_at DESC);

-- 11. Risk Rules
CREATE TABLE risk_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name VARCHAR(100) UNIQUE NOT NULL,
    condition_expression TEXT NOT NULL,
    risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    weight INTEGER NOT NULL CHECK (weight >= 0 AND weight <= 100),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    version INTEGER DEFAULT 1 NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 12. Sessions
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) UNIQUE NOT NULL,
    user_agent TEXT,
    ip_address VARCHAR(45),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
CREATE INDEX idx_sessions_user_id ON sessions(user_id);

-- 13. Hospital Settings
CREATE TABLE hospital_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_name VARCHAR(255) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata' NOT NULL,
    default_checklist_time TIME DEFAULT '09:00' NOT NULL,
    emergency_phone VARCHAR(20),
    dashboard_refresh_interval_min INTEGER DEFAULT 5 NOT NULL,
    escalation_sla_hours INTEGER DEFAULT 2 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
