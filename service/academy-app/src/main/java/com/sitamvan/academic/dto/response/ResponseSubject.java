package com.sitamvan.academic.dto.response;

import java.time.Instant;

public record ResponseSubject(Long id, String code, String name, String category,
                              Instant createdAt, Instant modifiedAt) { }
