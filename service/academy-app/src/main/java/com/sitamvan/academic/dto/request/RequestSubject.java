package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestSubject(
        @NotBlank(message = "Kode mata pelajaran wajib diisi") @Size(max = 30) String code,
        @NotBlank(message = "Nama mata pelajaran wajib diisi") @Size(max = 150) String name,
        @Size(max = 30) String category
) { }
