-- Employee codes are optional. Deleted records do not reserve their codes.
-- Keep uk_teacher_school_user: account linking remains a separate workflow.
ALTER TABLE teachers DROP CONSTRAINT uk_teacher_school_code;
UPDATE teachers SET employee_code = NULL WHERE employee_code IS NOT NULL AND btrim(employee_code) = '';
CREATE UNIQUE INDEX uk_teacher_school_live_employee_code
    ON teachers (school_id, lower(btrim(employee_code)))
    WHERE row_status = 1 AND employee_code IS NOT NULL;
