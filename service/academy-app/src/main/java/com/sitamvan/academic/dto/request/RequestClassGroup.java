package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.*;

/** Full replacement request. Grade levels 1–13 cover primary through four-year vocational school. */
public record RequestClassGroup(
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long majorId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(13) Short gradeLevel,
        @NotNull @Min(1900) @Max(2200) Integer cohortYear,
        @NotBlank @Size(max = 20) String section,
        @Positive Integer capacity
) { }
