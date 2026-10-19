# Legacy Scheduling Reference: JadwalKuliahSolut

Dokumen ini menyimpan hasil reverse engineering awal terhadap aplikasi jadwal sekolah lama. Gunakan sebagai referensi domain dan aturan bisnis saat merancang Academy System Backend; jangan menganggap database lama sebagai schema final atau sumber migrasi yang sudah bersih.

## Sumber

- Aplikasi: `JadwalKuliahSolut` (VB.NET Windows Forms, .NET Framework 4.8)
- Database:
  `C:\Users\Thinkpad T480S\pribadi\pribadi\kampus\skrip\JadwalKuliahSolut\JadwalKuliahSolut\JadwalKuliahSolut\bin\Debug\JadwalKuliah.mdb`
- Koneksi aplikasi: `OleDb` dengan Jet/ACE Access provider.
- Inspeksi: dilakukan read-only menggunakan UCanAccess karena provider Jet/ACE tidak tersedia pada environment inspeksi.
- File `.mdb` tidak diubah selama inspeksi.

## Snapshot isi database

Hasil pembacaan database pada saat dokumentasi:

| Tabel | Perkiraan isi | Jumlah baris |
|---|---|---:|
| `config` | konfigurasi tahun ajaran, angkatan, durasi time unit | 4 |
| `guru` | master guru | 25 |
| `guru_keahlian` | relasi guru dengan jurusan/keahlian | 9 |
| `guru_mapel` | relasi guru dengan mata pelajaran | 27 |
| `jadwal_guru` | slot availability/jadwal guru per tahun ajaran | 404 |
| `jadwal_pelajaran` | assignment jadwal final untuk kelas | 60 |
| `keahlian` | jurusan/program keahlian SMK | 9 |
| `kelas2` | kelompok kelas berdasarkan angkatan dan jurusan | 32 |
| `mapel` | mata pelajaran per tahun ajaran/tingkat/semester/jurusan | 94 |
| `time_table` | slot waktu sekolah per hari dan tahun ajaran | 103 |

Snapshot ini mengandung data parsial atau percobaan pada beberapa tahun ajaran. Anggap angka di atas sebagai konteks inspeksi, bukan target seed data.

## Model domain lama

### `config`

Menyimpan konfigurasi global tahun ajaran:

- `tahun_ajar`, contoh `2023-2024`
- `tahun_angkatan`, daftar angkatan yang aktif pada tahun ajaran
- `time_unit_value`, durasi dasar satu slot dalam menit

Pada data yang diperiksa, durasi dasar pernah bernilai 25 dan 45 menit.

### `keahlian`

Mewakili jurusan/program keahlian SMK. Field penting:

- nama jurusan
- nama program
- bidang
- singkatan
- status dihapus

Contoh singkatan yang ada: DKV, MP, OTKP, TI. Data juga memiliki beberapa jurusan tanpa kelas aktif, sehingga master jurusan tidak selalu berarti sudah dipakai.

### `kelas2`

Mewakili kelompok kelas konkret, bukan hanya tingkat `X`, `XI`, atau `XII`.

Field penting:

- nama kelas, contoh `OTKP2023-1`
- tahun angkatan
- jurusan/keahlian

Aplikasi menghitung angkatan berdasarkan tahun ajaran dan tingkat kelas. Untuk tingkat `X`, `XI`, dan `XII`, tahun angkatan dikurangi 0, 1, atau 2 dari tahun awal tahun ajaran.

### `mapel`

Mewakili mata pelajaran yang tersedia untuk kombinasi tertentu:

- tahun ajaran
- jurusan/keahlian
- tingkat kelas: `X`, `XI`, `XII`
- semester
- jumlah `time_unit`
- apakah membutuhkan lab
- status dihapus

`time_unit` penting karena satu mata pelajaran dapat membutuhkan beberapa slot waktu. `butuh_lab` menunjukkan kebutuhan fasilitas khusus, tetapi database lama belum memiliki master ruangan/lab.

### `guru`

Master guru dengan informasi:

- nama
- email
- nomor telepon
- aktif/tidak aktif
- internal/eksternal

### `guru_keahlian` dan `guru_mapel`

Keduanya adalah relasi kemampuan/otorisasi mengajar:

- `guru_keahlian`: guru terkait jurusan/keahlian
- `guru_mapel`: guru terkait mata pelajaran

Saat memilih guru untuk jadwal, aplikasi lama juga menampilkan keahlian dan mata pelajaran guru sebagai informasi pendukung.

### `time_table`

Menyimpan slot waktu berdasarkan tahun ajaran dan hari:

- hari
- nama kegiatan
- urutan slot dalam hari
- jam mulai dan selesai
- label waktu tampilan
- flag `pelajaran`
- durasi menit
- tahun ajaran

Tidak semua slot adalah pelajaran. Slot dapat mewakili kegiatan sekolah seperti upacara atau waktu non-pelajaran.

### `jadwal_guru`

Menyimpan slot yang dikaitkan ke guru pada tahun ajaran tertentu:

- guru
- slot `time_table`
- tahun ajaran
- nama guru
- `is_scheduled`

Secara konsep ini berfungsi sebagai availability/grid jadwal guru dan dipakai untuk membantu mendeteksi bentrok guru.

### `jadwal_pelajaran`

Menyimpan assignment final pelajaran:

- tahun ajaran
- semester
- guru dan nama guru
- kelas dan nama kelas
- mapel dan nama mapel
- slot `time_table`
- label waktu tampilan

Tabel ini menyimpan beberapa nama sebagai denormalized snapshot (`nama_guru`, `nama_mapel`, dan seterusnya). Pada backend baru, relasi sebaiknya memakai foreign key dan nama diturunkan lewat query/mapper, kecuali memang diperlukan audit snapshot.

## Flow scheduling aplikasi lama

Flow utama yang terlihat dari source code:

```text
Pilih tahun ajaran
  -> pilih jurusan
  -> pilih tingkat kelas X/XI/XII
  -> pilih semester
  -> pilih nama kelas konkret
  -> pilih hari dan slot waktu
  -> pilih guru yang tersedia pada slot tersebut
  -> pilih mata pelajaran
  -> simpan jadwal pelajaran
```

Guru dapat difilter berdasarkan jadwal yang sesuai dengan hari dan slot waktu. Saat memilih jadwal final, aplikasi memastikan slot pada guru sesuai dengan slot yang dipilih.

Assignment jadwal disimpan per slot waktu. Karena mapel memiliki `time_unit`, satu kegiatan belajar dapat membutuhkan beberapa baris slot atau beberapa slot yang berurutan.

## Aturan bisnis yang perlu dipertahankan atau divalidasi ulang

Minimal backend baru perlu mempertimbangkan:

1. Satu guru tidak boleh mengajar dua kelas pada slot waktu yang sama.
2. Satu kelas tidak boleh memiliki dua pelajaran pada slot waktu yang sama.
3. Satu ruangan/lab tidak boleh dipakai dua kelas pada slot waktu yang sama.
4. Guru sebaiknya hanya dipilih jika aktif.
5. Guru perlu memiliki relasi kompetensi jurusan dan/atau mapel yang sesuai.
6. Assignment harus berada pada tahun ajaran dan semester yang benar.
7. Jumlah slot yang dipakai perlu memenuhi `time_unit` mapel.
8. Slot non-pelajaran tidak boleh dipakai sebagai slot mapel biasa.
9. Mapel yang soft-deleted tidak boleh muncul pada pilihan aktif.
10. Mapel dengan `butuh_lab` perlu divalidasi terhadap ruangan/fasilitas yang sesuai.

## Gap database lama untuk kebutuhan SMK

Database lama belum terlihat memiliki:

- master ruangan, lab, kapasitas, dan fasilitas;
- kapasitas atau kebutuhan siswa per kelas;
- wali kelas;
- data siswa dan enrollment kelas;
- kalender akademik/libur;
- status/publikasi jadwal;
- versioning atau approval jadwal;
- aturan jam mengajar maksimum guru;
- aturan khusus blok praktik atau multi-slot yang eksplisit;
- constraint database yang cukup untuk menjamin bentrok tidak terjadi.

Entity di atas jangan langsung ditambahkan semuanya sebelum flow produk dan kebutuhan frontend dikonfirmasi, tetapi `room/lab` sangat mungkin dibutuhkan untuk konteks SMK.

## Rekomendasi mapping ke backend baru

Gunakan database lama sebagai referensi konsep, lalu normalisasi ulang menjadi domain baru yang lebih jelas:

```text
AcademicYear
Major
ClassGroup
Subject
Teacher
TeacherMajor
TeacherSubject
TimeSlot
Room
TeacherAvailability
Schedule
ScheduleEntry
SchoolActivity
```

Model awal yang lebih aman:

- `Schedule` sebagai konteks tahun ajaran, semester, dan kelas.
- `ScheduleEntry` sebagai pasangan guru-mapel-ruang-slot.
- `TimeSlot` tetap membedakan slot pelajaran dan non-pelajaran.
- `Subject` menyimpan kebutuhan durasi/time unit dan kebutuhan fasilitas.
- `Room` menyimpan tipe ruang dan fasilitas.
- Validasi bentrok dilakukan di service dan diperkuat dengan constraint/index database.

Ikuti pola project backend yang sudah ada: entity memakai base audit/soft-delete yang sesuai, endpoint memakai DTO, mapping memakai mapper, dan business validation berada di service.

## Hal yang harus diingat saat melanjutkan investigasi

- Jangan menyalin tabel `jadwal_pelajaran` mentah-mentah karena berisi nama denormalized.
- Jangan menganggap `kelas2` hanya berarti tingkat kelas; ia menyimpan kelas konkret berdasarkan angkatan dan jurusan.
- Jangan menganggap semua `time_table` adalah jam mapel.
- Jangan menganggap snapshot data tahun ajaran lama sudah bersih; ada data parsial/percobaan.
- Sebelum implementasi scheduling, telusuri lagi source code form dan service untuk aturan bentrok, pengulangan slot, dan cara aplikasi menghitung `time_unit`.
- Jika butuh memahami perilaku UI lama, jalankan executable di folder `bin\Debug` secara terpisah dari proses inspeksi database.
