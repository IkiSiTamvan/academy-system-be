CREATE UNIQUE INDEX uk_one_published_schedule_per_period
    ON schedule_versions(academic_period_id)
    WHERE status = 'PUBLISHED' AND row_status = 1;

CREATE UNIQUE INDEX uk_one_active_academic_year_per_school
    ON academic_years(school_id)
    WHERE active = TRUE AND row_status = 1;
