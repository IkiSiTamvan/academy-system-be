package com.sitamvan.academic.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

public record RequestRoom(
        @NotBlank(message = "Kode ruangan wajib diisi") @Size(max = 30) String code,
        @NotBlank(message = "Nama ruangan wajib diisi") @Size(max = 120) String name,
        @NotBlank(message = "Jenis ruangan wajib diisi") @Size(max = 30) String roomType,
        @Positive(message = "Kapasitas harus lebih dari nol") Integer capacity,
        @NotNull(message = "Daftar fasilitas wajib disertakan, boleh kosong")
        @Size(max = 100, message = "Maksimal 100 fasilitas per ruangan") List<@NotNull @Positive Long> facilityIds
) { }
