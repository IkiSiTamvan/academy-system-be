package com.sitamvan.academic.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record ResponseAcademicPeriod(Long id, Long academicYearId, String academicYearName,
        String code, String name, LocalDate startsOn, LocalDate endsOn,
        Instant createdAt, Instant modifiedAt) { }
