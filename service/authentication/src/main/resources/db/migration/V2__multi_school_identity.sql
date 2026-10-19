ALTER TABLE roles ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS platform_role BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ALTER COLUMN name TYPE VARCHAR(100);
ALTER TABLE roles ALTER COLUMN created_by SET DEFAULT 0;
ALTER TABLE roles ALTER COLUMN created_at SET DEFAULT now();

WITH normalized AS (
    SELECT id,
           LEFT(UPPER(REGEXP_REPLACE(name, '[^A-Za-z0-9]+', '_', 'g')), 20) AS candidate,
           ROW_NUMBER() OVER (PARTITION BY LEFT(UPPER(REGEXP_REPLACE(name, '[^A-Za-z0-9]+', '_', 'g')), 20) ORDER BY id) AS duplicate_number
    FROM roles
)
UPDATE roles r
SET code = CASE
    WHEN n.duplicate_number = 1 AND n.candidate IN ('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER', 'TEACHER', 'VIEWER') THEN n.candidate
    WHEN n.candidate = '' THEN 'LEGACY_ROLE_' || r.id
    ELSE 'LEGACY_' || r.id || '_' || n.candidate
END
FROM normalized n
WHERE r.id = n.id AND (r.code IS NULL OR r.code = '');

ALTER TABLE roles ALTER COLUMN code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_code ON roles(code);

CREATE TABLE schools (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(500),
    time_zone VARCHAR(50) NOT NULL DEFAULT 'Asia/Jakarta',
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_by BIGINT,
    modified_at TIMESTAMPTZ
);

CREATE TABLE school_memberships (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_by BIGINT,
    modified_at TIMESTAMPTZ,
    CONSTRAINT uk_membership_school_user UNIQUE (school_id, user_id),
    CONSTRAINT uk_membership_id_school UNIQUE (id, school_id)
);

CREATE TABLE school_membership_roles (
    id BIGSERIAL PRIMARY KEY,
    membership_id BIGINT NOT NULL REFERENCES school_memberships(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_by BIGINT,
    modified_at TIMESTAMPTZ,
    CONSTRAINT uk_membership_role UNIQUE (membership_id, role_id)
);

CREATE TABLE user_global_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_by BIGINT,
    modified_at TIMESTAMPTZ,
    CONSTRAINT uk_user_global_role UNIQUE (user_id, role_id)
);

CREATE TABLE school_invitations (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_by BIGINT,
    modified_at TIMESTAMPTZ
);

-- Keep existing users and role assignments reachable under one explicitly named tenant.
INSERT INTO schools(code, name, time_zone)
VALUES ('LEGACY', 'Legacy School', 'Asia/Jakarta');

INSERT INTO school_memberships(school_id, user_id)
SELECT (SELECT id FROM schools WHERE code = 'LEGACY'), u.id FROM users u
ON CONFLICT (school_id, user_id) DO NOTHING;

INSERT INTO school_membership_roles(membership_id, role_id)
SELECT m.id, ur.role_id
FROM user_roles ur
JOIN school_memberships m ON m.user_id = ur.user_id
JOIN roles r ON r.id = ur.role_id
WHERE m.school_id = (SELECT id FROM schools WHERE code = 'LEGACY')
  AND r.code <> 'PLATFORM_ADMIN'
ON CONFLICT (membership_id, role_id) DO NOTHING;

INSERT INTO roles(code, name, platform_role)
VALUES
    ('PLATFORM_ADMIN', 'Platform Administrator', TRUE),
    ('SCHOOL_ADMIN', 'School Administrator', FALSE),
    ('SCHEDULER', 'Scheduler', FALSE),
    ('TEACHER', 'Teacher', FALSE),
    ('VIEWER', 'Viewer', FALSE)
ON CONFLICT (code) DO NOTHING;

UPDATE roles SET platform_role = TRUE WHERE code = 'PLATFORM_ADMIN';

INSERT INTO user_global_roles(user_id, role_id)
SELECT ur.user_id, ur.role_id
FROM user_roles ur JOIN roles r ON r.id = ur.role_id
WHERE r.code = 'PLATFORM_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
