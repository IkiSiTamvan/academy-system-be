package com.sitamvan.academic;

import com.fasterxml.jackson.databind.JsonNode;
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
class TeacherIntegrationTest {
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
    void crudNormalizesFiltersAuditsAndReusesDeletedCodes() {
        var created = call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("employeeCode", " gr-01 ", "name", " Ibu Rina ", "email", "rina@academy.test",
                        "phone", " 08123456 ", "active", true, "external", false));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long id = created.getBody().path("result").path("id").asLong();
        assertThat(created.getBody().path("result").path("employeeCode").asText()).isEqualTo("GR-01");
        assertThat(created.getBody().path("result").path("name").asText()).isEqualTo("Ibu Rina");
        assertThat(created.getBody().path("result").path("phone").asText()).isEqualTo("08123456");
        assertThat(jdbc.queryForObject("select created_by from teachers where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.GET, "?q=rina&active=true&size=1", viewer, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.GET, "?active=false", viewer, schoolA, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "?q=%25", viewer, schoolA, null).getBody().path("result")).isEmpty();

        // Editing school master data must preserve an existing account association.
        long userId = jdbc.queryForObject("insert into users(username, password) values (?, 'unused') returning id", Long.class, "linked-" + UUID.randomUUID());
        jdbc.update("update teachers set user_id = ? where id = ?", userId, id);
        var edited = call(HttpMethod.PUT, "/" + id, scheduler, schoolA,
                Map.of("employeeCode", "GR-01", "name", "Rina revisi", "active", false, "external", true));
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edited.getBody().path("result").path("email").isNull()).isTrue();
        assertThat(edited.getBody().path("result").path("phone").isNull()).isTrue();
        assertThat(edited.getBody().path("result").path("external").asBoolean()).isTrue();
        assertThat(edited.getBody().path("result").has("userId")).isFalse();
        assertThat(jdbc.queryForObject("select user_id from teachers where id = ?", Long.class, id)).isEqualTo(userId);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select row_status from teachers where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, teacher("gr-01")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void enforcesRolesTenantIsolationAndLiveSchool() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, teacher("PRIVATE")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, teacher("DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, teacher("DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", otherScheduler, schoolB, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, teacher("PRIVATE")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        jdbc.update("update schools set row_status = 0 where id = ?", schoolA);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void optionalEmployeeCodesRemainOptionalButLiveDuplicatesAndInvalidInputsFail() {
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, teacher("GR-01")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, teacher("gr-01")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", otherScheduler, schoolB, teacher("GR-01")).getStatusCode()).isEqualTo(HttpStatus.OK);
        var noCode = call(HttpMethod.POST, "", scheduler, schoolA, teacher(" "));
        assertThat(noCode.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(noCode.getBody().path("result").path("employeeCode").isNull()).isTrue();
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("name", "Tanpa kode", "active", true, "external", false)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("name", "Guru", "email", "not-an-email", "active", true, "external", false)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("name", "Guru", "active", true)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, teacher("X".repeat(41))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void preventsDeletingTeacherWithCompetenciesOrAvailability() {
        long teacherId = call(HttpMethod.POST, "", scheduler, schoolA, teacher("USED")).getBody().path("result").path("id").asLong();
        long subjectId = jdbc.queryForObject("insert into subjects(school_id, code, name) values (?, 'MTK', 'Matematika') returning id", Long.class, schoolA);
        jdbc.update("insert into teacher_subjects(teacher_id, subject_id) values (?, ?)", teacherId, subjectId);
        assertThat(call(HttpMethod.DELETE, "/" + teacherId, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("delete from teacher_subjects where teacher_id = ?", teacherId);
        long majorId = jdbc.queryForObject("insert into majors(school_id, code, name) values (?, 'RPL', 'RPL') returning id", Long.class, schoolA);
        jdbc.update("insert into teacher_majors(teacher_id, major_id) values (?, ?)", teacherId, majorId);
        assertThat(call(HttpMethod.DELETE, "/" + teacherId, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("delete from teacher_majors where teacher_id = ?", teacherId);
        long yearId = jdbc.queryForObject("insert into academic_years(school_id, code, name, starts_on, ends_on) values (?, '2026', '2026', '2026-07-01', '2027-06-30') returning id", Long.class, schoolA);
        long periodId = jdbc.queryForObject("insert into academic_periods(academic_year_id, code, name, starts_on, ends_on) values (?, 'GANJIL', 'Ganjil', '2026-07-01', '2026-12-31') returning id", Long.class, yearId);
        long slotId = jdbc.queryForObject("insert into time_slots(school_id, academic_year_id, day_of_week, sequence_number, label, starts_at, ends_at, duration_minutes, kind) values (?, ?, 1, 1, 'Jam 1', '07:00', '07:45', 45, 'LESSON') returning id", Long.class, schoolA, yearId);
        jdbc.update("insert into teacher_availability(teacher_id, academic_period_id, time_slot_id, available) values (?, ?, ?, false)", teacherId, periodId, slotId);
        assertThat(call(HttpMethod.DELETE, "/" + teacherId, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private long school() {
        return jdbc.queryForObject("insert into schools(code, name) values (?, 'Teacher test school') returning id", Long.class, UUID.randomUUID().toString().substring(0, 20));
    }
    private String identity(String role, long schoolId) {
        String username = "test-" + UUID.randomUUID().toString().substring(0, 12);
        long userId = jdbc.queryForObject("insert into users(username, password) values (?, ?) returning id", Long.class, username, encoder.encode("test-password-only"));
        Long roleId = jdbc.queryForObject("select id from roles where code = ?", Long.class, role);
        long membershipId = jdbc.queryForObject("insert into school_memberships(school_id, user_id) values (?, ?) returning id", Long.class, schoolId, userId);
        jdbc.update("insert into school_membership_roles(membership_id, role_id) values (?, ?)", membershipId, roleId);
        var response = http.postForEntity("/v1/auth/login", Map.of("username", username, "password", "test-password-only"), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("result").path("accessToken").asText();
    }
    private Map<String, Object> teacher(String code) {
        return Map.of("employeeCode", code, "name", "Guru uji", "active", true, "external", false);
    }
    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, Long schoolId, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (schoolId != null) headers.set("X-School-ID", schoolId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/teachers" + suffix, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
