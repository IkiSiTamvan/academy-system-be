ALTER TABLE facilities DROP CONSTRAINT IF EXISTS uk_facility_school_code;
CREATE UNIQUE INDEX uk_facility_school_live_code ON facilities (school_id, lower(code)) WHERE row_status = 1;
