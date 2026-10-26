# Academy System Backend - Development Guide

Panduan ini menjelaskan pola standar saat menambahkan module, entity, endpoint, service, dan response baru ke project Academy System Backend.

Tujuan utamanya adalah menjaga agar setiap service memakai base utility yang sudah tersedia di module `library/shared-data`.

## 1. Struktur project

```text
academy-system/
├── library/
│   └── shared-data/
│       └── src/main/java/com/sitamvan/shared/data/
│           ├── base/       # Bentuk response API
│           ├── exception/  # Exception aplikasi dan handler global
│           └── util/       # Auditing, soft delete, response builder
└── service/
    └── <nama-service>/
        └── src/main/java/com/sitamvan/<service>/
            ├── config/
            ├── dto/request/
            ├── dto/response/
            ├── endpoint/
            ├── mapper/
            ├── model/
            ├── repository/
            ├── service/
            └── specifications/
```

Alur data yang dipakai:

```text
HTTP request
    -> Endpoint interface + implementation
    -> Service
    -> Repository
    -> Entity/database

Entity
    -> Mapper
    -> Response DTO
    -> ResponseBuilder
    -> BaseResponse JSON
```

## 2. Entity dan audit

### 2.1 Entity biasa: `AAuditable`

Gunakan `AAuditable` untuk entity yang membutuhkan informasi waktu dan user pembuat/perubahannya, tetapi tidak membutuhkan soft delete.

```java
@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room extends AAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
```

`AAuditable` menyediakan:

| Field | Keterangan |
|---|---|
| `createdBy` | ID user yang membuat data |
| `createdAt` | Waktu data dibuat |
| `modifiedBy` | ID user terakhir yang mengubah data |
| `modifiedAt` | Waktu perubahan terakhir |

Field audit disembunyikan dari serialisasi JSON dengan `@JsonIgnore`. Karena itu, expose field tersebut secara eksplisit lewat response DTO jika memang dibutuhkan oleh API.

### 2.2 Entity dengan soft delete: `ASoftDeletable`

Gunakan `ASoftDeletable` jika data tidak boleh benar-benar dihapus dari database.

```java
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student extends ASoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
```

`ASoftDeletable` mewarisi seluruh field audit dan menambahkan:

```java
private RowStatus rowStatus = RowStatus.ACTIVE;
```

`RowStatusConverter` sudah diberi `@Converter(autoApply = true)`, sehingga nilai akan disimpan sebagai:

| Enum | Nilai database |
|---|---:|
| `RowStatus.ACTIVE` | `1` |
| `RowStatus.DELETED` | `0` |

Saat soft delete, ubah statusnya:

```java
@Transactional
public void deleteStudent(Long id) {
    Student student = studentRepository.findById(id)
            .orElseThrow(() -> ServiceException.notFound("Student not found with id: " + id));

    student.setRowStatus(RowStatus.DELETED);
    studentRepository.save(student);
}
```

Jangan memakai `repository.delete(entity)` untuk entity soft-delete.

> Catatan: `ASoftDeletable` saat ini hanya menyediakan field dan converter. Query belum otomatis mengecualikan data `DELETED`. Tambahkan filter `rowStatus = ACTIVE` pada specification atau query repository setiap kali mengambil data.

Contoh specification:

```java
public static Specification<Student> activeOnly() {
    return (root, query, cb) ->
            cb.equal(root.get("rowStatus"), RowStatus.ACTIVE);
}
```

## 3. Auditing entity

Application utama sudah mengaktifkan:

```java
@EnableJpaAuditing
```

`@CreatedDate` dan `@LastModifiedDate` dapat diisi oleh Spring Data. Sementara itu, `@CreatedBy` dan `@LastModifiedBy` memerlukan bean `AuditorAware<Long>` yang mengambil ID user dari authentication context.

Contoh konfigurasi saat authentication sudah tersedia:

```java
@Bean
public AuditorAware<Long> auditorProvider() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .filter(UserPrincipal.class::isInstance)
            .map(UserPrincipal.class::cast)
            .map(UserPrincipal::getId);
}
```

Jika belum ada `AuditorAware`, jangan menganggap `createdBy`/`modifiedBy` otomatis terisi. Perhatikan juga bahwa `createdBy` di base class saat ini `nullable = false`.

## 4. DTO request dan response

Jangan expose entity JPA langsung sebagai response API. Gunakan DTO terpisah, sebaiknya Java `record`.

### Request DTO

Validasi diletakkan di request DTO:

```java
@Builder
public record RequestRoomCreate(
        @NotBlank(message = "Room name is required")
        @Size(max = 100, message = "Room name must not exceed 100 characters")
        String name,

        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity
) implements Serializable {
}
```

Untuk update, field yang boleh tidak diubah dapat nullable. Gunakan `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` di mapper agar nilai `null` tidak menimpa nilai lama.

### Response DTO

Response DTO hanya berisi field yang aman dan relevan untuk client:

```java
@Builder
public record ResponseRoom(
        Long id,
        String name,
        Integer capacity,
        Instant createdAt,
        Instant modifiedAt
) implements Serializable {
}
```

Jangan mengembalikan `password`, field internal, atau object relationship secara mentah.

## 5. Mapper dengan MapStruct

Semua konversi request/entity/response dilakukan di mapper, bukan manual di endpoint.

```java
@Mapper(componentModel = "spring")
public interface RoomMapper {

    Room toEntityFromCreate(RequestRoomCreate request);

    ResponseRoom toResponse(Room entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toEntityFromUpdate(RequestRoomUpdate request, @MappingTarget Room entity);
}
```

Jika nama field berbeda, gunakan `@Mapping`:

```java
@Mapping(target = "enabled", source = "isActive")
User toEntityFromCreate(RequestUserCreate request);
```

Business logic tetap berada di service. Contohnya, password harus di-encode di service setelah mapping, bukan di mapper.

## 6. Repository dan specification

Repository cukup berisi kemampuan query yang dibutuhkan oleh service.

```java
public interface IRoomRepository extends JpaRepository<Room, Long>,
        JpaSpecificationExecutor<Room> {
}
```

Untuk pencarian dengan banyak filter opsional, buat utility specification yang mengembalikan `null` ketika filter tidak dipakai.

```java
public final class RoomSpecifications {

    private RoomSpecifications() {}

    public static Specification<Room> nameLike(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String like = "%" + name.trim().toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), like);
    }
}
```

Gabungkan specification di service menggunakan `Specification.allOf(...)`, seperti pola yang sudah dipakai oleh `UserServiceImpl`.

## 7. Service layer

Service menjadi tempat business logic, transaksi, pengecekan data, dan pemetaan error database.

```java
public interface IRoomService {
    Room createRoom(RequestRoomCreate request);
    Room getRoomById(Long id);
    Room updateRoom(Long id, RequestRoomUpdate request);
    void deleteRoom(Long id);
    Page<Room> getRooms(RoomSearchCriteria criteria, Integer page, Integer size);
}
```

Implementasinya mengikuti pola berikut:

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements IRoomService {

    private final IRoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Override
    public Room createRoom(RequestRoomCreate request) {
        Room room = roomMapper.toEntityFromCreate(request);
        return roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> ServiceException.notFound(
                        "Room not found with id: " + id));
    }
}
```

Aturan service:

- Gunakan `@Transactional` untuk operasi yang mengubah data.
- Gunakan `@Transactional(readOnly = true)` untuk operasi baca.
- Gunakan `ServiceException` untuk error bisnis yang sudah diketahui.
- Jangan mengembalikan `BaseResponse` dari service; response adalah tanggung jawab endpoint.
- Jangan menerima atau mengembalikan entity dari layer endpoint tanpa mapper.
- Constraint database tetap harus dibuat walaupun sudah ada validasi di DTO.

## 8. Exception standar

Gunakan factory method dari `ServiceException`:

```java
throw ServiceException.notFound("Room not found with id: " + id);
throw ServiceException.duplicate("Room name already exists");
throw ServiceException.badRequest("Invalid room configuration");
throw ServiceException.unauthorized("Authentication is required");
throw ServiceException.forbidden("You are not allowed to access this room");
throw ServiceException.internal("Room schedule could not be generated");
```

Tidak perlu menangkap `ServiceException` di setiap endpoint. `GlobalExceptionHandler` sudah menangani exception tersebut secara terpusat.

Validasi request akan otomatis ditangani jika parameter endpoint diberi `@Valid`:

```java
BaseResponse<ResponseRoom> createRoom(
        @Valid @RequestBody RequestRoomCreate request);
```

> Catatan implementasi saat ini: `GlobalExceptionHandler.handleMyCustomException()` selalu mengirim HTTP code `400`, walaupun `ServiceException` menyimpan status `404`, `409`, `401`, atau `403`. Jika status factory ingin benar-benar berlaku, handler perlu menggunakan `ex.getStatus()`.

Perilaku yang diharapkan:

```java
@ExceptionHandler(ServiceException.class)
public ResponseEntity<BaseResponse<?>> handleServiceException(ServiceException ex) {
    return ResponseEntity
            .status(ex.getStatus())
            .body(ResponseBuilder.error(ex.getStatus(), ex.getMessage()));
}
```

## 9. Endpoint/controller

Deklarasikan route dan request mapping di interface endpoint. Implementasi endpoint hanya mengatur alur request, memanggil service, mapping response, lalu membungkus hasilnya.

```java
@RequestMapping("/v1/room")
public interface IRoomEndpoint {

    @PostMapping
    BaseResponse<ResponseRoom> createRoom(
            @Valid @RequestBody RequestRoomCreate request);

    @GetMapping("/{id}")
    BaseResponse<ResponseRoom> getRoomById(@PathVariable Long id);
}
```

```java
@RestController
@RequiredArgsConstructor
public class RoomEndpointImpl implements IRoomEndpoint {

    private final IRoomService roomService;
    private final RoomMapper roomMapper;

    @Override
    public BaseResponse<ResponseRoom> createRoom(RequestRoomCreate request) {
        Room room = roomService.createRoom(request);
        return ResponseBuilder.ok(
                roomMapper.toResponse(room),
                "Room created successfully");
    }

    @Override
    public BaseResponse<ResponseRoom> getRoomById(Long id) {
        Room room = roomService.getRoomById(id);
        return ResponseBuilder.ok(
                roomMapper.toResponse(room),
                "Room fetched successfully");
    }
}
```

Gunakan konstanta route di `ApplicationConstant` jika route dipakai berulang atau merupakan bagian dari public API.

## 10. Response standar

Gunakan `ResponseBuilder`, bukan membuat `BaseResponse` manual di setiap endpoint.

### Single result

```java
return ResponseBuilder.ok(responseRoom, "Room fetched successfully");
```

Contoh JSON:

```json
{
  "result": {
    "id": 1,
    "name": "Room A",
    "capacity": 30
  },
  "response": {
    "code": 200,
    "message": "Room fetched successfully",
    "success": true
  },
  "metaData": null
}
```

### List biasa

```java
return ResponseBuilder.ok(responseList, "Rooms fetched successfully");
```

### Pagination

Map entity page ke response DTO, lalu gunakan overload `Page`:

```java
Page<ResponseRoom> response = rooms.map(roomMapper::toResponse);
return ResponseBuilder.ok(response, "Rooms fetched successfully", null);
```

Response pagination akan memiliki metadata:

```json
"metaData": {
  "pageCount": 3,
  "totalData": 25
}
```

Parameter `page` bersifat zero-based, sehingga halaman pertama adalah `0`.

Sebaiknya validasi ukuran page dan membatasi nilai maksimalnya agar endpoint tidak menerima query yang terlalu besar.

## 11. Pola create/update/delete

### Create

1. Endpoint menerima request DTO dengan `@Valid`.
2. Service melakukan business validation tambahan jika diperlukan.
3. Mapper mengubah request menjadi entity.
4. Service mengisi field khusus, misalnya password ter-encode atau owner ID.
5. Repository menyimpan entity.
6. Endpoint mengubah entity menjadi response DTO dan memakai `ResponseBuilder.ok()`.

### Update

1. Service mencari entity berdasarkan ID.
2. Jika tidak ada, lempar `ServiceException.notFound(...)`.
3. Mapper mengubah field non-null ke entity dengan `@MappingTarget`.
4. Service menjalankan logic khusus.
5. Repository menyimpan entity.

### Delete

Untuk entity biasa:

```java
repository.delete(entity);
```

Untuk entity `ASoftDeletable`:

```java
entity.setRowStatus(RowStatus.DELETED);
repository.save(entity);
```

Pastikan query list/detail tidak mengembalikan data dengan status `DELETED`.

## 12. Testing

Minimal test service perlu mencakup:

- create berhasil dan entity tersimpan;
- get by ID berhasil;
- get by ID ketika data tidak ada menghasilkan `ServiceException.notFound`;
- update hanya mengubah field yang dikirim;
- password atau field sensitif diproses sesuai aturan;
- delete berhasil;
- delete ketika data tidak ada menghasilkan exception;
- duplicate/constraint database dipetakan menjadi error aplikasi.

Gunakan pola test yang sudah ada di:

```text
service/academy-app/src/test/java/com/sitamvan/auth/service/impl/UserServiceImplTest.java
```

Untuk unit test service, gunakan Mockito seperti `@Mock` dan `@InjectMocks`. Test endpoint dapat ditambahkan dengan `@WebMvcTest` jika ingin memverifikasi route, validasi request, dan bentuk response JSON.

## 13. Checklist menambahkan resource baru

- [ ] Tambahkan entity di `model`.
- [ ] Pilih `AAuditable` atau `ASoftDeletable` sesuai kebutuhan.
- [ ] Tambahkan constraint dan index database yang diperlukan.
- [ ] Buat request DTO di `dto/request`.
- [ ] Buat response DTO di `dto/response`.
- [ ] Buat mapper MapStruct dengan `componentModel = "spring"`.
- [ ] Buat repository.
- [ ] Buat specification jika ada filter pencarian.
- [ ] Buat interface service dan implementasinya.
- [ ] Tambahkan transaksi pada service.
- [ ] Gunakan `ServiceException` untuk error bisnis.
- [ ] Buat endpoint interface dan implementasinya.
- [ ] Gunakan `@Valid` untuk request body.
- [ ] Gunakan `ResponseBuilder` untuk semua response.
- [ ] Pastikan entity tidak dikembalikan langsung ke client.
- [ ] Tambahkan unit test service.
- [ ] Jalankan test dan cek generated mapper.

## 14. Konvensi singkat

| Bagian | Konvensi |
|---|---|
| Entity | `User`, `Room`, `Schedule` |
| Repository | `IUserRepository`, `IRoomRepository` |
| Service interface | `IUserService` |
| Service implementation | `UserServiceImpl` |
| Endpoint interface | `IUserEndpoint` |
| Endpoint implementation | `UserEndpointImpl` |
| Request DTO | `RequestUserCreate`, `RequestUserUpdate` |
| Response DTO | `ResponseUser` |
| Mapper | `UserMapper` |
| Search specification | `UserSpecifications` |

Gunakan nama field yang konsisten antara entity, DTO, mapper, specification, dan database. Jika memang berbeda, dokumentasikan mapping-nya dengan `@Mapping`.

## 15. Konteks arsitektur Academy System

Untuk keputusan domain, batas fase, auth multi-sekolah, dan daftar pekerjaan lanjutan, mulai dari [docs/architecture.md](docs/architecture.md). Catatan domain legacy tetap ada di `docs/legacy-schedule-mdb-domain.md`; itu referensi perilaku lama, bukan schema final.

- Aplikasi tetap satu Spring Boot modular dan satu PostgreSQL database. Data sekolah harus selalu dibatasi oleh membership dan `X-School-ID` yang tervalidasi server.
- Akun berlaku global; role catalog juga global dan sama untuk semua sekolah. Pemberian role sekolah melekat ke membership. `PLATFORM_ADMIN` adalah role global.
- `Subject` adalah katalog sekolah. `ClassSubjectRequirement` menyimpan kebutuhan untuk kelas-periode: `sessionsPerWeek` dan `slotsPerSession` terpisah. `slotsPerSession` adalah durasi satu sesi dalam slot, seperti `time_unit` di sistem lama.
- Migrasi schema memakai Flyway. Jangan mengubah `ddl-auto: validate` ke `update` untuk menambal schema.
- Konfigurasi lokal/server memakai `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, dan optional bootstrap admin env vars. Jangan commit secret.
- Repo ini adalah proyek belajar: gunakan fitur Java 21 bila meningkatkan keterbacaan dan perbaiki domain legacy. Untuk repo kerja, prioritaskan compatibility dengan sistem existing.
- Setiap dua commit atau di batas fitur, tinjau pola berulang yang nyata sebelum menambahkan helper ke `shared-data`.
