# Academic masters

All endpoints use base `/api`, bearer authentication, and trusted school context from the JWT filter validating `X-School-ID`. Reads allow `PLATFORM_ADMIN`, `SCHOOL_ADMIN`, `SCHEDULER`, `TEACHER`, and `VIEWER`; mutations allow the first three roles. Cross-school record IDs return 404. Disabled schools are rejected. Records are soft deleted; audit history remains. Standard response envelopes follow the academic-year API.

Each collection supports GET list and POST create; `/{id}` supports GET, PUT full replacement, and DELETE. Lists accept literal `q` (max100), zero-based `page` (default0), `size` (default20,1–100). Invalid input returns400, duplicate live codes409. All mutations lock the selected school to serialize related-record changes. Codes are trimmed/uppercased and unique case-insensitively for live records; deleted codes may be reused.

| Collection | Body fields | Additional list filters |
| --- | --- | --- |
| `/v1/academic-periods` | academicYearId, code, name, startsOn, endsOn | academicYearId |
| `/v1/majors` | code, name, programName?, fieldName? | — |
| `/v1/subjects` | code, name, category? | — |
| `/v1/teachers` | employeeCode?, name, email?, phone?, active, external | active |
| `/v1/class-groups` | academicYearId, majorId, code, name, gradeLevel, cohortYear, section, capacity? | academicYearId, majorId |
| `/v1/facilities` | code, name | — |
| `/v1/rooms` | code, name, roomType, capacity?, facilityIds | — |
| `/v1/time-slots` | academicYearId, dayOfWeek, sequenceNumber, label, startsAt, endsAt, kind | academicYearId, dayOfWeek |

`?` means nullable/optional value. Room `facilityIds` is a required array (may be empty, max100 distinct positive IDs). Selected relations must be live and belong to the school. No user-account assignment is introduced through teacher CRUD; existing user links are preserved.

Periods must fit within their year, and end date must follow start date. Period codes are unique per year. Classes require grade1–13, cohort1900–2200, nonblank section, and optional positive capacity; class codes are unique per school/year. Major/year reassignment is blocked when the class is already referenced. Used periods cannot change year.

Room capacity is optional and positive. The UI offers `CLASSROOM`, `LAB`, and `OTHER` room types. Responses include `facilityIds` and `facilityNames`. Updating links preserves retained link audits. Rooms referenced by schedules cannot change capacity/type/facilities or be deleted; descriptive name/code edits remain allowed.

Time slots use Monday1 through Sunday7 and positive sequence numbers unique per year/day. Kinds: `LESSON`, `BREAK`, `ACTIVITY`, `OTHER`. Times must be whole minutes within one day, end after start. Duration is derived by the server. Overlap in the same school/year/day is rejected; adjacent slots are allowed. Responses include `dayName`, `kindLabel`, and `durationMinutes`. Slots used by availability or live schedules cannot change structural fields or be deleted; label corrections remain allowed.

Deletion guards also protect majors used by classes/qualifications, subjects used by qualifications/requirements, teachers used by qualifications/availability/schedules, periods used by requirements/availability/schedules, and facilities used by live rooms. Qualification/availability/requirement screens and schedule generation are outside this master delivery.

Flyway V6–V13 replaces original full code constraints with partial live unique indexes. Java21 + Docker are required for `mvn verify`, which uses disposable PostgreSQL containers separate from local Compose data.
