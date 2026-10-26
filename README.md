# Academy System Backend

Spring Boot 3 backend for a multi-school academic scheduling system. The application is a modular monolith backed by PostgreSQL.

## Start here

- [Architecture and continuation context](docs/architecture.md)
- [Development conventions](DEVELOPMENT_GUIDE.md)
- [Legacy scheduling domain reference](docs/legacy-schedule-mdb-domain.md)

## Runtime configuration

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` (at least 32 bytes). The default access-token lifetime is 24 hours. To bootstrap the first platform administrator, also set `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`; `BOOTSTRAP_ADMIN_EMAIL` is optional. Flyway applies schema changes at startup and Hibernate validates the resulting schema.

For local development, the application also loads an optional `application-local.yml` from the working directory. This file is Git-ignored and stays outside the packaged JAR. You can keep a generated local JWT secret and local database settings there; do not commit credentials. Run from the repository root so this file is found. `JWT_SECRET` should contain at least 32 bytes and should be randomly generated.

The executable module is `service/academy-app`, started by `com.sitamvan.AcademyApplication`. Auth, academic, and scheduling packages run in the same application. Nine academic master screens are integrated with Next.js; scheduling configuration and generation remain future work.

All routes share the `/api` servlet context (default port `8081`):

- Login: `POST http://localhost:8081/api/v1/auth/login`
- Current user: `GET http://localhost:8081/api/v1/auth/me`
- Schools: `GET` / `POST http://localhost:8081/api/v1/schools`
- Academic years: `GET` / `POST http://localhost:8081/api/v1/academic-years`, `GET` / `PUT` / `DELETE` on `/{id}`
- Swagger UI: `http://localhost:8081/api/swagger-ui/index.html`

Clients previously using `/api/auth` as the base path must switch to `/api`.

## First integrated master

See [academic-year API](docs/academic-years.md). The sibling `schedule-system-fe` provides login, school selection/creation, and academic-year management at `http://localhost:3000`. Next.js forwards bearer tokens and school context on the server.

The remaining eight master endpoints are documented in [academic masters](docs/academic-masters.md): periods, majors, subjects, teachers, class groups, facilities, rooms, and time slots. Each supports school-scoped CRUD and integrates with the same frontend.

`mvn verify` requires Java 21 and Docker for PostgreSQL Testcontainers. Its disposable test database is separate from the local Compose database. Coverage includes migrations, HTTP CRUD, tenant isolation, roles, validation, soft deletion, and active-year transitions.

## Build and run

### Local PostgreSQL with Docker

Copy `.env.example` to `.env` and set `DB_PASSWORD`, then start the database:

```sh
docker compose up -d --wait
docker compose ps
```

PostgreSQL 17 is available at `localhost:5432`, database `academy_system`, user `academy` by default. Set matching credentials in the Git-ignored root `application-local.yml` alongside your JWT settings:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/academy_system}
    username: ${DB_USERNAME:academy}
    password: ${DB_PASSWORD:YOUR_LOCAL_DATABASE_PASSWORD}
```

Compose reads `.env`; Spring Boot reads `application-local.yml` or environment variables. Keep their database settings consistent. Flyway creates the application tables when the backend starts.

Data is stored in the named Docker volume `academy-system_postgres_data`. `docker compose stop` or `docker compose down` preserves it; `docker compose start` or `docker compose up -d --wait` starts the database again. `docker compose down -v` deletes the database volume and its data. Changing the password in `.env` does not change a password in an already initialized database.

### Backend

Use JDK 21 and Maven. From the repository root, with PostgreSQL running and the environment variables above configured:

```sh
mvn clean verify
java -jar service/academy-app/target/academy-app-1.0.0-SNAPSHOT.jar
```

For an IDE run configuration, select `com.sitamvan.AcademyApplication` with the `academy-app` module classpath and the repository root (`$PROJECT_DIR$`) as the working directory.
