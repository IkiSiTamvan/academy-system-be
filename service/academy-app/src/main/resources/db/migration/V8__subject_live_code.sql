ALTER TABLE subjects DROP CONSTRAINT uk_subject_school_code;
CREATE UNIQUE INDEX uk_subject_live_school_code ON subjects(school_id, lower(code)) WHERE row_status = 1;
