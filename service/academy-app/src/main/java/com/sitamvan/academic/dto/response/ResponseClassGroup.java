package com.sitamvan.academic.dto.response;

import java.time.Instant;

public record ResponseClassGroup(Long id, Long academicYearId, String academicYearName,
        Long majorId, String majorName, String code, String name, short gradeLevel,
        int cohortYear, String section, Integer capacity, Instant createdAt, Instant modifiedAt) { }
