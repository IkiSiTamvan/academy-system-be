package com.sitamvan.academic.dto.response;

import com.sitamvan.academic.model.TimeSlot;
import java.time.Instant;
import java.time.LocalTime;

public record ResponseTimeSlot(Long id, Long academicYearId, String academicYearName,
        short dayOfWeek, String dayName, short sequenceNumber, String label,
        LocalTime startsAt, LocalTime endsAt, short durationMinutes, TimeSlot.Kind kind,
        String kindLabel, Instant createdAt, Instant modifiedAt) { }
