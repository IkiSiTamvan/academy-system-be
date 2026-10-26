package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RequestAcademicPeriod(
        @NotNull(message = "Tahun ajaran wajib dipilih") @Positive Long academicYearId,
        @NotBlank(message = "Kode periode wajib diisi") @Size(max = 30) String code,
        @NotBlank(message = "Nama periode wajib diisi") @Size(max = 100) String name,
        @NotNull(message = "Tanggal mulai wajib diisi") LocalDate startsOn,
        @NotNull(message = "Tanggal selesai wajib diisi") LocalDate endsOn
) { }
