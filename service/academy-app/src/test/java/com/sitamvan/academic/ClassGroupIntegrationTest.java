package com.sitamvan.academic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "academy.bootstrap.platform-admin.username=",
        "academy.bootstrap.platform-admin.password=", "jwt.secret=integration-test-signing-secret-at-least-32-bytes"
})
class ClassGroupIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    long schoolA;
    long schoolB;
    String scheduler;
    String viewer;
    String otherScheduler;

    @BeforeEach
    void fixtures() {
        schoolA = school();
        schoolB = school();
        scheduler = identity("SCHEDULER", schoolA);
        viewer = identity("VIEWER", schoolA);
        otherScheduler = identity("SCHEDULER", schoolB);
    }

    @Test
    void createsFiltersUpdatesSoftDeletesAndReusesCodes() {
        long year = year(schoolA, "2026");
        long major = major(schoolA);
        var created = call(HttpMethod.POST, "", scheduler, schoolA, group(year, major, "  x-rpl-1  "));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        var row = created.getBody().path("result");
        long id = row.path("id").asLong();
        assertThat(row.path("code").asText()).isEqualTo("X-RPL-1");
        assertThat(row.path("academicYearName").asText()).isEqualTo("Year 2026");
        assertThat(row.path("majorName").asText()).isEqualTo("Rekayasa perangkat lunak");
        assertThat(row.path("createdAt").asText()).isNotBlank();
        assertThat(jdbc.queryForObject("select created_by from class_groups where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.GET, "?q=rpl&academicYearId=" + year + "&majorId=" + major, scheduler, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.GET, "?q=%25", scheduler, schoolA, null).getBody().path("result")).isEmpty();
        var edit = new java.util.HashMap<>(group(year, major, "EDIT"));
        edit.put("name", "  Kelas revisi  ");
        edit.remove("capacity");
        var updated = call(HttpMethod.PUT, "/" + id, scheduler, schoolA, edit);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("result").path("name").asText()).isEqualTo("Kelas revisi");
        assertThat(updated.getBody().path("result").path("capacity").isNull()).isTrue();
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("select row_status from class_groups where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, group(year, major, "EDIT")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void checksRelationsValidationCodeScopeAndRoles() {
        long yearA = year(schoolA, "2026");
        long yearB = year(schoolB, "2026");
        long majorA = major(schoolA);
        long majorB = major(schoolB);
        long id = call(HttpMethod.POST, "", scheduler, schoolA, group(yearA, majorA, "PRIVATE")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, group(yearA, majorA, "private")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        long secondYear = year(schoolA, "2027");
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, group(secondYear, majorA, "PRIVATE")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, group(yearB, majorA, "YEAR")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, group(yearA, majorB, "MAJOR")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "?majorId=" + majorB, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, group(yearA, majorA, "DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, group(yearA, majorA, "DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, group(yearB, majorB, "STOLEN")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var invalid = new java.util.HashMap<>(group(yearA, majorA, "INVALID"));
        for (String field : java.util.List.of("gradeLevel", "cohortYear", "capacity")) {
            invalid.put(field, 0);
            assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            invalid = new java.util.HashMap<>(group(yearA, majorA, "INVALID"));
        }
        invalid.put("gradeLevel", 14);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("update majors set row_status = 0 where id = ?", majorA);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, group(yearA, majorA, "DELETED")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void protectsUsedClassesFromDeletionAndYearOrMajorChanges() {
        long year = year(schoolA, "2026");
        long major = major(schoolA);
        long id = call(HttpMethod.POST, "", scheduler, schoolA, group(year, major, "USED")).getBody().path("result").path("id").asLong();
        long period = jdbc.queryForObject("insert into academic_periods(academic_year_id, code, name, starts_on, ends_on) values (?, 'S1', 'Semester 1', '2026-07-01', '2026-12-31') returning id", Long.class, year);
        long subject = jdbc.queryForObject("insert into subjects(school_id, code, name) values (?, 'MTK', 'Matematika') returning id", Long.class, schoolA);
        long requirement = jdbc.queryForObject("insert into class_subject_requirements(school_id, class_group_id, academic_period_id, subject_id, sessions_per_week, slots_per_session) values (?, ?, ?, ?, 2, 2) returning id", Long.class, schoolA, id, period, subject);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        long nextYear = year(schoolA, "2027");
        long nextMajor = major(schoolA);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, group(nextYear, major, "USED")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, group(year, nextMajor, "USED")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, group(year, major, "RENAMED")).getStatusCode()).isEqualTo(HttpStatus.OK);
        jdbc.update("update class_subject_requirements set row_status = 0 where id = ?", requirement);
        long teacher = jdbc.queryForObject("insert into teachers(school_id, name) values (?, 'Guru') returning id", Long.class, schoolA);
        long version = jdbc.queryForObject("insert into schedule_versions(school_id, academic_period_id, version_number, name) values (?, ?, 1, 'Draft') returning id", Long.class, schoolA, period);
        long entry = jdbc.queryForObject("insert into schedule_entries(schedule_version_id, requirement_id, teacher_id, meeting_number) values (?, ?, ?, 1) returning id", Long.class, version, requirement, teacher);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("update schedule_entries set row_status = 0 where id = ?", entry);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private long year(long schoolId, String code) {
        return jdbc.queryForObject("insert into academic_years(school_id, code, name, starts_on, ends_on) values (?, ?, ?, '2026-07-01', '2027-06-30') returning id", Long.class, schoolId, code, "Year " + code);
    }
    private long major(long schoolId) {
        return jdbc.queryForObject("insert into majors(school_id, code, name) values (?, ?, 'Rekayasa perangkat lunak') returning id", Long.class, schoolId, UUID.randomUUID().toString().substring(0, 20));
    }
    private Map<String, Object> group(long year, long major, String code) {
        return Map.of("academicYearId", year, "majorId", major, "code", code, "name", "Kelas " + code, "gradeLevel", 10, "cohortYear", 2026, "section", "1", "capacity", 36);
    }
    private long school() {
        return jdbc.queryForObject("insert into schools(code, name) values (?, 'Integration school') returning id", Long.class, UUID.randomUUID().toString().substring(0, 20));
    }

    private String identity(String role, long schoolId) {
        String username = "test-" + UUID.randomUUID().toString().substring(0, 12);
        long userId = jdbc.queryForObject("insert into users(username, password) values (?, ?) returning id", Long.class, username, encoder.encode("test-password-only"));
        Long roleId = jdbc.queryForObject("select id from roles where code = ?", Long.class, role);
        if (role.equals("PLATFORM_ADMIN")) {
            jdbc.update("insert into user_global_roles(user_id, role_id) values (?, ?)", userId, roleId);
        } else {
            long membershipId = jdbc.queryForObject("insert into school_memberships(school_id, user_id) values (?, ?) returning id", Long.class, schoolId, userId);
            jdbc.update("insert into school_membership_roles(membership_id, role_id) values (?, ?)", membershipId, roleId);
        }
        var response = http.postForEntity("/v1/auth/login", Map.of("username", username, "password", "test-password-only"), JsonNode.class);
        assertThat(response.getStatusCode()).as("Login failure: %s", response.getBody()).isEqualTo(HttpStatus.OK);
        JsonNode login = response.getBody();
        assertThat(login.path("result").path("accessToken").asText()).isNotBlank();
        return login.path("result").path("accessToken").asText();
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, Long schoolId, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (schoolId != null) headers.set("X-School-ID", schoolId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/class-groups" + suffix, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
