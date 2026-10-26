ALTER TABLE academic_periods DROP CONSTRAINT uk_period_year_code;
CREATE UNIQUE INDEX uk_period_year_live_code
    ON academic_periods (academic_year_id, lower(code)) WHERE row_status = 1;
