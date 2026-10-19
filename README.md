# Academy System Backend

Spring Boot 3 backend for a multi-school academic scheduling system. The application is a modular monolith backed by PostgreSQL.

## Start here

- [Architecture and continuation context](docs/architecture.md)
- [Development conventions](DEVELOPMENT_GUIDE.md)
- [Legacy scheduling domain reference](docs/legacy-schedule-mdb-domain.md)

## Runtime configuration

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` (at least 32 bytes). The default access-token lifetime is 24 hours. To bootstrap the first platform administrator, also set `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`; `BOOTSTRAP_ADMIN_EMAIL` is optional. Flyway applies schema changes at startup and Hibernate validates the resulting schema.

The current executable module is `service/authentication`. Routes use the existing `/api/auth` servlet context. Academic and scheduling data models are present; CRUD workflows and automatic schedule generation are future work.
