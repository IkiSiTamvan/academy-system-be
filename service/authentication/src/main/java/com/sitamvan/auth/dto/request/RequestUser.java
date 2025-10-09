package com.sitamvan.auth.dto.request;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record RequestUser(
        String username,
        String email,
        String password,
        Boolean isActive
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 23435322323L;
}
