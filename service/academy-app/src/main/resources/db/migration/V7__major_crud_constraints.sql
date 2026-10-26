ALTER TABLE majors DROP CONSTRAINT IF EXISTS uk_major_school_code;
CREATE UNIQUE INDEX uk_major_school_live_code ON majors (school_id, lower(code)) WHERE row_status = 1;
