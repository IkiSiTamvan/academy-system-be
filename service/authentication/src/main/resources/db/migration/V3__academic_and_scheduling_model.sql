CREATE TABLE academic_years (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_academic_year_school_code UNIQUE (school_id, code),
    CONSTRAINT uk_academic_year_id_school UNIQUE (id, school_id),
    CONSTRAINT ck_academic_year_dates CHECK (ends_on > starts_on)
);

CREATE TABLE academic_periods (
    id BIGSERIAL PRIMARY KEY,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_period_year_code UNIQUE (academic_year_id, code),
    CONSTRAINT uk_period_id_year UNIQUE (id, academic_year_id),
    CONSTRAINT ck_academic_period_dates CHECK (ends_on > starts_on)
);

CREATE TABLE majors (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    program_name VARCHAR(150),
    field_name VARCHAR(150),
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_major_school_code UNIQUE (school_id, code),
    CONSTRAINT uk_major_id_school UNIQUE (id, school_id)
);

CREATE TABLE class_groups (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    major_id BIGINT NOT NULL REFERENCES majors(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    grade_level SMALLINT NOT NULL,
    cohort_year INTEGER NOT NULL,
    section VARCHAR(20) NOT NULL,
    capacity INTEGER,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_class_group_school_year_code UNIQUE (school_id, academic_year_id, code),
    CONSTRAINT ck_class_group_grade CHECK (grade_level > 0),
    CONSTRAINT ck_class_group_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(30),
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_subject_school_code UNIQUE (school_id, code),
    CONSTRAINT uk_subject_id_school UNIQUE (id, school_id)
);

CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    user_id BIGINT REFERENCES users(id),
    employee_code VARCHAR(40),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    external BOOLEAN NOT NULL DEFAULT FALSE,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_teacher_school_code UNIQUE (school_id, employee_code),
    CONSTRAINT uk_teacher_school_user UNIQUE (school_id, user_id),
    CONSTRAINT uk_teacher_id_school UNIQUE (id, school_id)
);

CREATE TABLE teacher_subjects (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_teacher_subject UNIQUE (teacher_id, subject_id)
);

CREATE TABLE teacher_majors (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    major_id BIGINT NOT NULL REFERENCES majors(id),
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_teacher_major UNIQUE (teacher_id, major_id)
);

CREATE TABLE class_subject_requirements (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    class_group_id BIGINT NOT NULL REFERENCES class_groups(id),
    academic_period_id BIGINT NOT NULL REFERENCES academic_periods(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    sessions_per_week SMALLINT NOT NULL,
    slots_per_session SMALLINT NOT NULL,
    requires_lab BOOLEAN NOT NULL DEFAULT FALSE,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_requirement_class_period_subject UNIQUE (class_group_id, academic_period_id, subject_id),
    CONSTRAINT ck_requirement_sessions CHECK (sessions_per_week > 0),
    CONSTRAINT ck_requirement_duration CHECK (slots_per_session > 0)
);

CREATE TABLE time_slots (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    day_of_week SMALLINT NOT NULL,
    sequence_number SMALLINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    starts_at TIME NOT NULL,
    ends_at TIME NOT NULL,
    duration_minutes SMALLINT NOT NULL,
    kind VARCHAR(16) NOT NULL,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_time_slot_year_day_sequence UNIQUE (academic_year_id, day_of_week, sequence_number),
    CONSTRAINT ck_time_slot_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_time_slot_times CHECK (ends_at > starts_at),
    CONSTRAINT ck_time_slot_duration CHECK (duration_minutes > 0)
);

CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    room_type VARCHAR(30) NOT NULL,
    capacity INTEGER,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_room_school_code UNIQUE (school_id, code)
);

CREATE TABLE facilities (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_facility_school_code UNIQUE (school_id, code)
);

CREATE TABLE room_facilities (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    facility_id BIGINT NOT NULL REFERENCES facilities(id),
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_room_facility UNIQUE (room_id, facility_id)
);

CREATE TABLE teacher_availability (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    academic_period_id BIGINT NOT NULL REFERENCES academic_periods(id),
    time_slot_id BIGINT NOT NULL REFERENCES time_slots(id),
    available BOOLEAN NOT NULL,
    note VARCHAR(255),
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_teacher_availability_period_slot UNIQUE (teacher_id, academic_period_id, time_slot_id)
);

CREATE TABLE schedule_versions (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools(id),
    academic_period_id BIGINT NOT NULL REFERENCES academic_periods(id),
    version_number INTEGER NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_schedule_version_period_number UNIQUE (academic_period_id, version_number),
    CONSTRAINT ck_schedule_version_number CHECK (version_number > 0),
    CONSTRAINT ck_schedule_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE schedule_entries (
    id BIGSERIAL PRIMARY KEY,
    schedule_version_id BIGINT NOT NULL REFERENCES schedule_versions(id),
    requirement_id BIGINT NOT NULL REFERENCES class_subject_requirements(id),
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    room_id BIGINT REFERENCES rooms(id),
    meeting_number SMALLINT NOT NULL,
    row_status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_schedule_entry_requirement_meeting UNIQUE (schedule_version_id, requirement_id, meeting_number),
    CONSTRAINT ck_schedule_entry_meeting CHECK (meeting_number > 0)
);

CREATE TABLE schedule_entry_slots (
    id BIGSERIAL PRIMARY KEY,
    schedule_entry_id BIGINT NOT NULL REFERENCES schedule_entries(id),
    schedule_version_id BIGINT NOT NULL REFERENCES schedule_versions(id),
    class_group_id BIGINT NOT NULL REFERENCES class_groups(id),
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    room_id BIGINT REFERENCES rooms(id),
    time_slot_id BIGINT NOT NULL REFERENCES time_slots(id),
    slot_order SMALLINT NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), modified_by BIGINT, modified_at TIMESTAMPTZ,
    CONSTRAINT uk_schedule_class_slot UNIQUE (schedule_version_id, class_group_id, time_slot_id),
    CONSTRAINT uk_schedule_teacher_slot UNIQUE (schedule_version_id, teacher_id, time_slot_id),
    CONSTRAINT uk_schedule_room_slot UNIQUE (schedule_version_id, room_id, time_slot_id),
    CONSTRAINT ck_schedule_entry_slot_order CHECK (slot_order > 0)
);

CREATE INDEX ix_membership_user ON school_memberships(user_id, enabled);
CREATE INDEX ix_class_groups_school_year ON class_groups(school_id, academic_year_id);
CREATE INDEX ix_requirements_period_class ON class_subject_requirements(academic_period_id, class_group_id);
CREATE INDEX ix_schedule_versions_period_status ON schedule_versions(academic_period_id, status);
