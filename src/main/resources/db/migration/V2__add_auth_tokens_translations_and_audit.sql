-- V2: authentication tokens, career/localization translations, and soft-delete audit columns

-- =========================================================================
-- Verification tokens (password reset / email verification)
-- =========================================================================
CREATE TABLE verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    token_type  VARCHAR(50)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT verification_tokens_type_check CHECK (
        token_type IN ('PASSWORD_RESET', 'EMAIL_VERIFICATION')
    )
);

CREATE INDEX idx_verification_tokens_user_id ON verification_tokens (user_id);
CREATE INDEX idx_verification_tokens_token_hash ON verification_tokens (token_hash);

-- =========================================================================
-- Career cluster translations
-- NOTE: career_cluster_id and language_id reference tables that are not yet
-- introduced (career_clusters, languages). They are stored as UUID columns
-- here; FK constraints will be added in the migration that creates those
-- base tables to avoid referencing non-existent relations.
-- =========================================================================
CREATE TABLE career_cluster_translations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    career_cluster_id  UUID         NOT NULL,
    language_id        UUID         NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT career_cluster_translations_unique UNIQUE (career_cluster_id, language_id)
);

CREATE INDEX idx_career_cluster_translations_cluster_id ON career_cluster_translations (career_cluster_id);
CREATE INDEX idx_career_cluster_translations_language_id ON career_cluster_translations (language_id);

-- =========================================================================
-- Career translations
-- NOTE: career_id and language_id reference tables that are not yet
-- introduced (careers, languages); stored as UUID columns for now.
-- =========================================================================
CREATE TABLE career_translations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    career_id          UUID         NOT NULL,
    language_id        UUID         NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    education_pathway  TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT career_translations_unique UNIQUE (career_id, language_id)
);

CREATE INDEX idx_career_translations_career_id ON career_translations (career_id);
CREATE INDEX idx_career_translations_language_id ON career_translations (language_id);

-- =========================================================================
-- Soft-delete audit columns
-- Requested for tenants, org_units, users. These columns are also added to
-- roles, permissions, and user_roles because they share the BaseEntity
-- @MappedSuperclass; Hibernate ddl-auto=validate requires every mapped table
-- to expose the new columns.
-- =========================================================================
ALTER TABLE tenants    ADD COLUMN deleted_at TIMESTAMP, ADD COLUMN deleted_by UUID;
ALTER TABLE org_units  ADD COLUMN deleted_at TIMESTAMP, ADD COLUMN deleted_by UUID;
ALTER TABLE users      ADD COLUMN deleted_at TIMESTAMP, ADD COLUMN deleted_by UUID;

ALTER TABLE roles      ADD COLUMN deleted_at TIMESTAMP, ADD COLUMN deleted_by UUID;
ALTER TABLE permissions ADD COLUMN deleted_at TIMESTAMP, ADD COLUMN deleted_by UUID;
ALTER TABLE user_roles ADD COLUMN deleted_at TIMESTAMP, ADD COLUMN deleted_by UUID;
