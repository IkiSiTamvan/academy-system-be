ALTER TABLE class_groups DROP CONSTRAINT IF EXISTS uk_class_group_school_year_code;
CREATE UNIQUE INDEX uk_class_group_live_code
    ON class_groups (school_id, academic_year_id, lower(code)) WHERE row_status = 1;
