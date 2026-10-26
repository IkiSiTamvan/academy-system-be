package com.sitamvan.academic.dto.request;

import com.sitamvan.academic.model.TimeSlot;
import jakarta.validation.constraints.*;
import java.time.LocalTime;

/** Full editable state; duration is always derived by the service. */
public record RequestTimeSlot(
        @NotNull @Positive Long academicYearId,
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        @NotNull @Min(1) @Max(32767) Short sequenceNumber,
        @NotBlank @Size(max = 100) String label,
        @NotNull LocalTime startsAt,
        @NotNull LocalTime endsAt,
        @NotNull TimeSlot.Kind kind
) { }
