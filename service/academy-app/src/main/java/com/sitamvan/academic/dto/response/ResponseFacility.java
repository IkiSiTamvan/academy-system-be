package com.sitamvan.academic.dto.response;

import java.time.Instant;

public record ResponseFacility(Long id, String code, String name, Instant createdAt, Instant modifiedAt) { }
