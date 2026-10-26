ALTER TABLE time_slots DROP CONSTRAINT uk_time_slot_year_day_sequence;

CREATE UNIQUE INDEX uk_time_slot_live_year_day_sequence
    ON time_slots (school_id, academic_year_id, day_of_week, sequence_number)
    WHERE row_status = 1;
