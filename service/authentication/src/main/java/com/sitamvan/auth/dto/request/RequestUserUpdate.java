package com.sitamvan.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record RequestUserUpdate(

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Email(message = "Email should be valid")
        String email,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        Boolean isActive
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
