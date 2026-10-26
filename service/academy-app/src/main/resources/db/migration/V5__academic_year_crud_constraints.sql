-- A soft-deleted code may be reused; live codes are case-insensitive per school.
ALTER TABLE academic_years DROP CONSTRAINT uk_academic_year_school_code;
CREATE UNIQUE INDEX uk_academic_year_school_live_code
    ON academic_years (school_id, lower(code)) WHERE row_status = 1;

-- Reconcile pre-existing flags deterministically before enforcing one active year.
WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY school_id ORDER BY starts_on DESC, id DESC) AS position
    FROM academic_years WHERE active = TRUE AND row_status = 1
)
UPDATE academic_years SET active = FALSE WHERE id IN (SELECT id FROM ranked WHERE position > 1);

CREATE UNIQUE INDEX uk_academic_year_school_active
    ON academic_years (school_id) WHERE active = TRUE AND row_status = 1;
