ALTER TABLE rooms DROP CONSTRAINT uk_room_school_code;
CREATE UNIQUE INDEX uk_room_live_school_code ON rooms (school_id, lower(code)) WHERE row_status = 1;
