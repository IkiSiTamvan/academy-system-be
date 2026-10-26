# Academy System Backend: Architecture Context

This document is the short handoff and decision record for continuing this repository. The legacy MDB notes in `legacy-schedule-mdb-domain.md` remain a reference to the old school scheduling workflow, not a target schema.

## Product and architecture decisions

- Build one Spring Boot application and PostgreSQL database as a modular monolith. Use `service/academy-app` as the runnable composition root and separate domain code by packages; do not add a Quarkus service or solver worker yet.
- Support multiple schools in one database. School-owned records carry a school relationship. Every future tenant-facing service/repository must scope reads and writes to the authenticated membership; a client supplied school ID is context, never proof of access.
- A user account is global and may belong to several schools. The role catalog is global and fixed across schools. Role assignments are per school membership, so the same account can be `SCHEDULER` at one school and `TEACHER` at another. `PLATFORM_ADMIN` is a global role.
- Academic years and periods are configured by each school. School masters such as subjects and majors are school-owned and may be copied through a future template workflow.
- Schedule versions belong to an academic period and move through draft, published, and archived states. Weekly timetable generation is not implemented yet.
- A subject is a reusable school catalog entry. `ClassSubjectRequirement` binds a subject to a concrete class and period and stores `sessionsPerWeek` separately from `slotsPerSession`. `slotsPerSession` is the legacy `time_unit`: duration in consecutive timetable slots, and may differ by class, grade, and major.
- Teacher-subject and teacher-major qualifications are separate relationships. A future scheduling rule still needs to decide whether both qualifications are required or either is sufficient.
- Time slots are classified as lesson, break, activity, or other. School activities such as assembly or duha are not ordinary lesson assignments.
- The initial access token remains stateless with the configured 24-hour lifetime. It contains the user ID; school context is passed in `X-School-ID`, and membership/roles are looked up for each request. Logout is client-side token removal; there is no refresh token or server-side revocation table.

## Current implementation shape

- `service/academy-app` is the executable Spring Boot module, started by `com.sitamvan.AcademyApplication`. `com.sitamvan.academic.model` contains school, academic, room, teacher, and scheduling entities; `com.sitamvan.auth` contains identity and tenant membership. Auth is a domain package within the application, not a separately deployed service.
- `library/shared-data` holds cross-cutting response/error and audit/soft-delete primitives. Do not move domain rules there. Review for a reusable helper after every two commits or at a feature boundary, and only extract a pattern demonstrated in multiple places.
- Flyway owns schema changes. Hibernate uses `ddl-auto: validate`. The first migrations create the auth baseline, add tenant membership, preserve legacy role links under a `LEGACY` school, and create the academic/scheduling tables.
- Bootstrap the first platform admin with `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`, and optionally `BOOTSTRAP_ADMIN_EMAIL`. The seeded role catalog includes `PLATFORM_ADMIN`, `SCHOOL_ADMIN`, `SCHEDULER`, `TEACHER`, and `VIEWER`.
- All routes use the shared servlet context `/api`: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/invitations/accept`, `POST /api/v1/schools`, and `POST /api/v1/schools/invitations`. Clients previously using `/api/auth` as their base path must switch to `/api`. Invitation creation returns a one-time token for out-of-band delivery; no email provider is wired yet.
- `POST /v1/user` and the other user management routes now require `PLATFORM_ADMIN`. Other routes require a valid bearer token. Tenant roles are loaded from the membership matching `X-School-ID`.
- `created_by` falls back to system actor ID `0` for seed/bootstrap operations that do not have a logged-in user.

## Model inventory

Identity and tenancy: `users`, `roles`, `schools`, `school_memberships`, `school_membership_roles`, `user_global_roles`, and `school_invitations`.

Academic masters and requirements: `academic_years`, `academic_periods`, `majors`, `class_groups`, `subjects`, `teachers`, `teacher_subjects`, `teacher_majors`, `class_subject_requirements`, `time_slots`, `rooms`, `facilities`, `room_facilities`, and `teacher_availability`.

Schedule state: `schedule_versions`, `schedule_entries`, and `schedule_entry_slots`. Slot rows carry class, teacher, room, and version keys so unique constraints can reject collisions.

The solver-independent `ScheduleGenerator` port accepts a school/period/version request and returns proposed requirement/teacher/room/slot assignments. It has no implementation or HTTP endpoint yet.

## Follow-up work

- Add tenant-scoped CRUD/use cases for academic masters and validate that related IDs all belong to the same school and academic period. Current phase adds the data model, not those APIs.
- Add integration coverage for Flyway against PostgreSQL, role-scoped access, invitation expiry/reuse, and class/teacher/room slot conflicts.
- Review timetable requirement semantics with schools: sessions per week are modeled separately from duration in slots; decide whether exact weekly frequency, minimum/maximum, or preferred frequency is needed.
- Define qualification matching, teacher availability default behavior, room facility matching, class promotion/reorganization, and whether a session may span breaks.
- Compare OR-Tools and Timefold against representative school constraints before choosing or implementing a solver. Keep generation behind an interface so it can move to a worker later if CPU or latency requires it.
- Configure production `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a random `JWT_SECRET` of at least 32 bytes. Never commit real credentials.

## Compatibility and learning preference

This repository is the user's learning project: prefer current Java 21 language features when they improve clarity, and improve the domain rather than copying legacy flaws. For the user's work repositories, preserve compatibility with the existing system as the default priority.
