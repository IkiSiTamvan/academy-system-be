package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT replaces every editable facility field. */
public record RequestFacility(
        @NotBlank(message = "Kode fasilitas wajib diisi") @Size(max = 30) String code,
        @NotBlank(message = "Nama fasilitas wajib diisi") @Size(max = 120) String name
) { }
