package com.sitamvan.academic.dto.response;

import java.time.Instant;
import java.util.List;

public record ResponseRoom(Long id, String code, String name, String roomType, Integer capacity,
        List<Long> facilityIds, List<String> facilityNames, Instant createdAt, Instant modifiedAt) { }
