package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT replaces all editable fields, including optional program and field names. */
public record RequestMajor(
        @NotBlank(message = "Kode jurusan wajib diisi") @Size(max = 30) String code,
        @NotBlank(message = "Nama jurusan wajib diisi") @Size(max = 150) String name,
        @Size(max = 150) String programName,
        @Size(max = 150) String fieldName
) { }
