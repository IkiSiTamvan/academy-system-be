package com.sitamvan.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestAcceptInvitation(@NotBlank String token,
                                      @NotBlank @Size(min = 3, max = 50) String username,
                                      @NotBlank @Size(min = 8, max = 100) String password) { }
