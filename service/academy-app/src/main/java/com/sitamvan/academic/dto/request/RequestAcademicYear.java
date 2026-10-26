package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** PUT replaces the editable fields; null is never a partial update. */
public record RequestAcademicYear(
        @NotBlank(message = "Kode tahun ajaran wajib diisi") @Size(max = 20) String code,
        @NotBlank(message = "Nama tahun ajaran wajib diisi") @Size(max = 100) String name,
        @NotNull(message = "Tanggal mulai wajib diisi") LocalDate startsOn,
        @NotNull(message = "Tanggal selesai wajib diisi") LocalDate endsOn,
        @NotNull(message = "Status aktif wajib diisi") Boolean active
) { }
