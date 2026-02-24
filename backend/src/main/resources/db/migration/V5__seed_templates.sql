-- Seed recovery templates for TKR, THR, ACL
-- References admin user from V3 as created_by

INSERT INTO recovery_templates (surgery_type, display_name, checklist_config, milestone_config, mandatory_image_days, monitoring_days, is_active, created_by, created_at, updated_at)
VALUES
(
  'TKR',
  'Total Knee Replacement',
  '{
    "days": {
      "1": {"questions": ["pain_score", "swelling_level", "fever_check", "medication_adherence"]},
      "2": {"questions": ["pain_score", "swelling_level", "fever_check", "mobility_check", "medication_adherence"]},
      "3": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence", "wound_image"]},
      "4": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]},
      "5": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence", "wound_image"]},
      "6": {"questions": ["pain_score", "swelling_level", "fever_check", "mobility_check", "medication_adherence"]},
      "7": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]},
      "14": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]}
    }
  }'::jsonb,
  '{
    "milestones": {
      "3": {"expected": "Patient able to bend knee 30 degrees", "label": "Early Flexion"},
      "5": {"expected": "Patient walking with walker support", "label": "Assisted Walking"},
      "7": {"expected": "Pain score should be below discharge level", "label": "Pain Management"},
      "14": {"expected": "Independent walking with minimal support", "label": "Independence Goal"}
    }
  }'::jsonb,
  '{3,5}',
  14,
  true,
  (SELECT id FROM users WHERE email = 'admin@orthowatch.com'),
  NOW(),
  NOW()
),
(
  'THR',
  'Total Hip Replacement',
  '{
    "days": {
      "1": {"questions": ["pain_score", "swelling_level", "fever_check", "medication_adherence"]},
      "2": {"questions": ["pain_score", "swelling_level", "fever_check", "mobility_check", "medication_adherence"]},
      "3": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence", "wound_image"]},
      "4": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]},
      "5": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence", "wound_image"]},
      "6": {"questions": ["pain_score", "swelling_level", "fever_check", "mobility_check", "medication_adherence"]},
      "7": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]},
      "14": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]}
    }
  }'::jsonb,
  '{
    "milestones": {
      "3": {"expected": "Patient sitting upright with support", "label": "Sitting Up"},
      "5": {"expected": "Patient walking with walker, no hip dislocation signs", "label": "Safe Mobility"},
      "7": {"expected": "Hip precautions followed, pain decreasing", "label": "Precaution Compliance"},
      "14": {"expected": "Walking with cane, stair climbing attempted", "label": "Functional Recovery"}
    }
  }'::jsonb,
  '{3,5}',
  14,
  true,
  (SELECT id FROM users WHERE email = 'admin@orthowatch.com'),
  NOW(),
  NOW()
),
(
  'ACL',
  'ACL Reconstruction',
  '{
    "days": {
      "1": {"questions": ["pain_score", "swelling_level", "fever_check", "medication_adherence"]},
      "2": {"questions": ["pain_score", "swelling_level", "fever_check", "mobility_check", "medication_adherence"]},
      "3": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence", "wound_image"]},
      "4": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]},
      "5": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence", "wound_image"]},
      "6": {"questions": ["pain_score", "swelling_level", "fever_check", "mobility_check", "medication_adherence"]},
      "7": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]},
      "14": {"questions": ["pain_score", "swelling_level", "fever_check", "dvt_symptoms", "mobility_check", "medication_adherence"]}
    }
  }'::jsonb,
  '{
    "milestones": {
      "3": {"expected": "Full extension achieved, minimal swelling", "label": "Extension Goal"},
      "5": {"expected": "Crutch walking, quad activation exercises", "label": "Early Rehab"},
      "7": {"expected": "Wound healing well, brace locked in extension for walking", "label": "Wound Check"},
      "14": {"expected": "90 degrees flexion, able to do straight leg raise", "label": "Range of Motion"}
    }
  }'::jsonb,
  '{3,5}',
  14,
  true,
  (SELECT id FROM users WHERE email = 'admin@orthowatch.com'),
  NOW(),
  NOW()
)
ON CONFLICT (surgery_type) DO NOTHING;
