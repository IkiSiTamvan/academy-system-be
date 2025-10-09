package com.sitamvan.auth.dto.response;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Builder
public record ResponseUser(
        Long id,
        String username,
        String email,
        Boolean isActive,
        Instant createdAt,
        Instant modifiedAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 23435322323L;
}
