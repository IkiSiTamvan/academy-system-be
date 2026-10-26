package com.sitamvan.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestLogin(@NotBlank String username, @NotBlank String password) { }
