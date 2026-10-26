# Academic-year API

Base URL: `http://localhost:8081/api`. Academic-year requests require `Authorization: Bearer <token>` and `X-School-ID: <school id>`. The JWT filter checks membership before attaching trusted school context. Platform admins can select any live school. Without school context, school members receive 403 (no school role); platform admins receive 400.

`GET /v1/schools` returns live schools visible to the account. Creating a school (`POST /v1/schools`) requires `PLATFORM_ADMIN`.

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/v1/academic-years` | Search live years in selected school |
| GET | `/v1/academic-years/{id}` | Read one year |
| POST | `/v1/academic-years` | Create year |
| PUT | `/v1/academic-years/{id}` | Replace all editable fields |
| DELETE | `/v1/academic-years/{id}` | Soft delete unused, inactive year |

List parameters: `q` (literal code/name substring, maximum 100 characters), `active` (optional boolean), `page` (zero-based, default 0), `size` (1–100, default 20). Sort: start date descending, ID descending. Standard response envelope contains `result`, `response`, and list `metaData.pageCount` / `metaData.totalData`.

Create/update body:

```json
{
  "code": "TA-2026",
  "name": "Tahun Ajaran 2026/2027",
  "startsOn": "2026-07-13",
  "endsOn": "2027-06-25",
  "active": true
}
```

Code (maximum 20) and name (maximum 100) must be nonblank; dates and active flag are required. End date must follow start date. Codes are trimmed, uppercased, and unique case-insensitively among live years within a school; deleted codes may be reused. Updated dates must still contain existing academic periods.

At most one live year is active per school; activating another deactivates the previous one in the same transaction. School row locks serialize mutations and a partial unique index enforces the invariant. A school may have no active year.

Platform admins, school admins, and schedulers can write. Teachers and viewers can read. Cross-school record IDs return 404; unauthorized school selection returns 403. Validation errors return 400; duplicate live codes return 409. Active years and years referenced by live periods, class groups, or time slots cannot be deleted. Auditing and soft deletion use existing shared primitives.

The frontend uses HttpOnly, SameSite=Lax cookies and server actions. JWTs are not stored in local storage. Production cookies default to Secure and require HTTPS. Logout removes the browser cookie; token revocation and refresh are not implemented.
