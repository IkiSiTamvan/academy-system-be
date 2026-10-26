package com.sitamvan.academic.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record ResponseAcademicYear(Long id, String code, String name,
                                   LocalDate startsOn, LocalDate endsOn, boolean active,
                                   Instant createdAt, Instant modifiedAt) { }
