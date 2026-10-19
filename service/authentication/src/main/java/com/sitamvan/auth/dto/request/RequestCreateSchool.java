package com.sitamvan.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestCreateSchool(@NotBlank @Size(max = 30) String code,
                                 @NotBlank @Size(max = 160) String name,
                                 @Size(max = 500) String address,
                                 @Size(max = 50) String timeZone) { }
