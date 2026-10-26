package com.sitamvan.auth.dto.request;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
public record UserSearchCriteria(
        String query,
        String username,
        String email,
        String role,
        Boolean enabled,
        List<Long> ids
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
