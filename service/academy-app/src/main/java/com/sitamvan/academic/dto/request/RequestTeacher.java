package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.*;

public record RequestTeacher(
        @Size(max = 40) String employeeCode,
        @NotBlank(message = "Nama guru wajib diisi") @Size(max = 150) String name,
        @Email(message = "Format email tidak valid") @Size(max = 255) String email,
        @Size(max = 40) String phone,
        @NotNull(message = "Status aktif wajib diisi") Boolean active,
        @NotNull(message = "Status guru luar wajib diisi") Boolean external
) { }
