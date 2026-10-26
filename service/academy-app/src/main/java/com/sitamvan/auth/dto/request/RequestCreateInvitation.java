package com.sitamvan.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestCreateInvitation(@NotBlank @Email String email, @NotBlank String roleCode) { }
