package com.sitamvan.academic.dto.response;

import java.time.Instant;

public record ResponseMajor(Long id, String code, String name, String programName,
                            String fieldName, Instant createdAt, Instant modifiedAt) { }
