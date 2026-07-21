-- Core tenant, org unit, user, and RBAC schema

CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    branding_config JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE org_units (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenants (id),
    parent_org_unit_id  UUID         REFERENCES org_units (id),
    org_unit_type       VARCHAR(50)  NOT NULL,
    code                VARCHAR(100) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT org_units_type_check CHECK (
        org_unit_type IN ('STATE', 'DISTRICT', 'INSTITUTION', 'SCHOOL', 'PROGRAM', 'COHORT')
    ),
    CONSTRAINT org_units_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE INDEX idx_org_units_tenant_id ON org_units (tenant_id);
CREATE INDEX idx_org_units_parent_id ON org_units (parent_org_unit_id);

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenants (id),
    primary_org_unit_id UUID         REFERENCES org_units (id),
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    user_type           VARCHAR(50)  NOT NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT users_type_check CHECK (
        user_type IN ('STUDENT', 'FACILITATOR', 'ADMIN', 'SUB_ADMIN', 'SUPER_ADMIN')
    ),
    CONSTRAINT users_status_check CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING')
    ),
    CONSTRAINT users_tenant_email_unique UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_primary_org_unit_id ON users (primary_org_unit_id);

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT roles_name_check CHECK (
        name IN ('STUDENT', 'FACILITATOR', 'ADMIN', 'SUB_ADMIN', 'SUPER_ADMIN')
    )
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id),
    permission_id UUID NOT NULL REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id),
    role_id     UUID        NOT NULL REFERENCES roles (id),
    scope_type  VARCHAR(50) NOT NULL DEFAULT 'GLOBAL',
    scope_id    UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT user_roles_scope_type_check CHECK (
        scope_type IN ('GLOBAL', 'TENANT', 'ORG_UNIT', 'CLUSTER')
    ),
    CONSTRAINT user_roles_user_role_scope_unique UNIQUE (user_id, role_id, scope_type, scope_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE auth_attempts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    tenant_id     UUID         REFERENCES tenants (id),
    ip_address    VARCHAR(45),
    user_agent    TEXT,
    status        VARCHAR(20)  NOT NULL,
    failure_reason VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT auth_attempts_status_check CHECK (
        status IN ('SUCCESS', 'FAILURE')
    )
);

CREATE INDEX idx_auth_attempts_email ON auth_attempts (email);
CREATE INDEX idx_auth_attempts_created_at ON auth_attempts (created_at);

-- Seed RBAC roles and permissions

INSERT INTO permissions (code, description) VALUES
    ('USER_READ', 'View users'),
    ('USER_WRITE', 'Create and update users'),
    ('ORG_UNIT_READ', 'View organizational units'),
    ('ORG_UNIT_WRITE', 'Create and update organizational units'),
    ('TENANT_READ', 'View tenant configuration'),
    ('TENANT_WRITE', 'Manage tenant configuration');

INSERT INTO roles (name, description) VALUES
    ('STUDENT', 'Student user with read-only self access'),
    ('FACILITATOR', 'Facilitator with scoped read access'),
    ('ADMIN', 'Tenant administrator'),
    ('SUB_ADMIN', 'Sub-administrator with org hierarchy scope'),
    ('SUPER_ADMIN', 'Platform super administrator');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('USER_READ', 'USER_WRITE', 'ORG_UNIT_READ', 'ORG_UNIT_WRITE', 'TENANT_READ')
WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('USER_READ', 'ORG_UNIT_READ')
WHERE r.name IN ('SUB_ADMIN', 'FACILITATOR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'USER_READ'
WHERE r.name = 'STUDENT';
