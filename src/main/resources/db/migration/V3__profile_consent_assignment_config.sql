-- V3: profiles, consent, refresh tokens, clusters, assignment, config, languages + enrichments

-- =========================================================================
-- Languages (created early so user FKs can reference them)
-- =========================================================================
CREATE TABLE languages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    native_name VARCHAR(100),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    deleted_by  UUID
);

INSERT INTO languages (code, name, native_name, is_default) VALUES
    ('en', 'English', 'English', TRUE),
    ('hi', 'Hindi', 'हिन्दी', FALSE);

-- =========================================================================
-- Configuration groups & values
-- =========================================================================
CREATE TABLE configuration_groups (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID REFERENCES tenants (id),
    code              VARCHAR(100) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    description       TEXT,
    is_system_defined BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        UUID,
    updated_by        UUID,
    deleted_at        TIMESTAMP,
    deleted_by        UUID,
    CONSTRAINT configuration_groups_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE TABLE configurations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    configuration_group_id  UUID         NOT NULL REFERENCES configuration_groups (id),
    code                    VARCHAR(100) NOT NULL,
    value                   VARCHAR(200) NOT NULL,
    display_order           INT          NOT NULL DEFAULT 0,
    metadata                JSONB,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID,
    updated_by              UUID,
    deleted_at              TIMESTAMP,
    deleted_by              UUID,
    CONSTRAINT configurations_group_code_unique UNIQUE (configuration_group_id, code)
);

INSERT INTO configuration_groups (code, name, description, is_system_defined) VALUES
    ('GENDER', 'Gender', 'Gender options', TRUE),
    ('GRADE', 'Grade', 'Student grade/class values', TRUE),
    ('AGE_GROUP', 'Age Group', 'Age group bands', TRUE),
    ('INSTITUTION_TYPE', 'Institution Type', 'Institution classification', TRUE),
    ('ASSESSMENT_LANGUAGE', 'Assessment Language', 'Assessment language preference', TRUE),
    ('PLATFORM_LANGUAGE', 'Platform Language', 'Platform UI language preference', TRUE);

INSERT INTO configurations (configuration_group_id, code, value, display_order)
SELECT g.id, v.code, v.value, v.ord
FROM configuration_groups g
JOIN (VALUES
    ('GENDER', 'MALE', 'Male', 1),
    ('GENDER', 'FEMALE', 'Female', 2),
    ('GENDER', 'OTHER', 'Other', 3),
    ('GRADE', 'GRADE_9', 'Grade 9', 1),
    ('GRADE', 'GRADE_10', 'Grade 10', 2),
    ('GRADE', 'GRADE_11', 'Grade 11', 3),
    ('GRADE', 'GRADE_12', 'Grade 12', 4),
    ('AGE_GROUP', 'AGE_13_15', '13-15', 1),
    ('AGE_GROUP', 'AGE_16_18', '16-18', 2)
) AS v(group_code, code, value, ord) ON g.code = v.group_code;

-- =========================================================================
-- Enrich existing tables
-- =========================================================================
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS logo_url TEXT;

ALTER TABLE org_units
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS metadata JSONB;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(100),
    ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS gender_config_id UUID REFERENCES configurations (id),
    ADD COLUMN IF NOT EXISTS preferred_platform_language_id UUID REFERENCES languages (id),
    ADD COLUMN IF NOT EXISTS preferred_assessment_language_id UUID REFERENCES languages (id),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

UPDATE users SET first_name = COALESCE(first_name, split_part(email, '@', 1)) WHERE first_name IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_user_type ON users (user_type);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_mobile ON users (mobile_number);

-- =========================================================================
-- User profiles
-- =========================================================================
CREATE TABLE user_profiles (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL UNIQUE REFERENCES users (id),
    grade_config_id      UUID REFERENCES configurations (id),
    age_group_config_id  UUID REFERENCES configurations (id),
    guardian_name        VARCHAR(150),
    guardian_mobile      VARCHAR(20),
    guardian_email       VARCHAR(255),
    address_line_1       TEXT,
    address_line_2       TEXT,
    city                 VARCHAR(100),
    state                VARCHAR(100),
    pincode              VARCHAR(20),
    profile_data         JSONB,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================================
-- Consent records
-- =========================================================================
CREATE TABLE consent_records (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID         NOT NULL REFERENCES users (id),
    consent_type          VARCHAR(50)  NOT NULL,
    consent_text_version  VARCHAR(50)  NOT NULL,
    guardian_name         VARCHAR(150),
    guardian_contact      VARCHAR(50),
    consent_given         BOOLEAN      NOT NULL,
    consent_given_at      TIMESTAMPTZ,
    consent_withdrawn_at  TIMESTAMPTZ,
    metadata              JSONB,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT consent_records_type_check CHECK (consent_type IN ('SELF', 'GUARDIAN'))
);

CREATE INDEX idx_consent_records_user_id ON consent_records (user_id);

-- =========================================================================
-- Refresh tokens (persisted)
-- =========================================================================
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  TEXT         NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- =========================================================================
-- Organizational clusters
-- =========================================================================
CREATE TABLE organizational_clusters (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL REFERENCES tenants (id),
    code         VARCHAR(100) NOT NULL,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    cluster_type VARCHAR(50)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   UUID,
    updated_by   UUID,
    deleted_at   TIMESTAMP,
    deleted_by   UUID,
    CONSTRAINT organizational_clusters_type_check CHECK (
        cluster_type IN ('REGION', 'PROGRAM', 'CUSTOM')
    ),
    CONSTRAINT organizational_clusters_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE TABLE organizational_cluster_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cluster_id  UUID        NOT NULL REFERENCES organizational_clusters (id),
    member_type VARCHAR(50) NOT NULL,
    member_id   UUID        NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    added_by    UUID REFERENCES users (id),
    CONSTRAINT organizational_cluster_members_type_check CHECK (
        member_type IN ('ORG_UNIT', 'USER')
    ),
    CONSTRAINT organizational_cluster_members_unique UNIQUE (cluster_id, member_type, member_id)
);

CREATE INDEX idx_cluster_members_member ON organizational_cluster_members (member_type, member_id);

-- =========================================================================
-- Facilitator–student assignment
-- =========================================================================
CREATE TABLE facilitator_student_assignments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants (id),
    facilitator_id UUID         NOT NULL REFERENCES users (id),
    student_id     UUID         NOT NULL REFERENCES users (id),
    org_unit_id    UUID         REFERENCES org_units (id),
    assigned_by    UUID         REFERENCES users (id),
    assigned_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    unassigned_at  TIMESTAMPTZ,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_fsa_active_unique
    ON facilitator_student_assignments (facilitator_id, student_id)
    WHERE is_active = TRUE;

CREATE INDEX idx_fsa_facilitator ON facilitator_student_assignments (facilitator_id);
CREATE INDEX idx_fsa_student ON facilitator_student_assignments (student_id);

CREATE TABLE assignment_history (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id      UUID         NOT NULL REFERENCES facilitator_student_assignments (id),
    student_id         UUID         NOT NULL REFERENCES users (id),
    old_facilitator_id UUID         REFERENCES users (id),
    new_facilitator_id UUID         REFERENCES users (id),
    action             VARCHAR(50)  NOT NULL,
    performed_by       UUID         REFERENCES users (id),
    performed_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    remarks            TEXT,
    CONSTRAINT assignment_history_action_check CHECK (
        action IN ('ASSIGNED', 'REASSIGNED', 'UNASSIGNED')
    )
);

CREATE INDEX idx_assignment_history_student ON assignment_history (student_id);
