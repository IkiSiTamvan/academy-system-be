package com.sitamvan.academic;

import com.fasterxml.jackson.databind.JsonNode;
import com.sitamvan.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
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
class AcademicPeriodIntegrationTest {
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
    @Autowired JwtService jwt;
    long schoolA;
    long schoolB;
    long yearA;
    long yearB;
    String scheduler;
    String otherScheduler;
    String viewer;

    @BeforeEach
    void fixtures() {
        schoolA = school();
        schoolB = school();
        yearA = year(schoolA, "YEAR-A");
        yearB = year(schoolB, "YEAR-B");
        scheduler = identity("SCHEDULER", schoolA);
        otherScheduler = identity("SCHEDULER", schoolB);
        viewer = identity("VIEWER", schoolA);
    }

    @Test
    void crudPreservesAuditAndAllowsDeletedCodeReuse() {
        var created = call(HttpMethod.POST, "", scheduler, schoolA, body(yearA, " ganjil "));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = created.getBody().path("result");
        long id = result.path("id").asLong();
        assertThat(result.path("code").asText()).isEqualTo("GANJIL");
        assertThat(result.path("academicYearId").asLong()).isEqualTo(yearA);
        assertThat(result.path("academicYearName").asText()).isEqualTo("Tahun ajaran");
        assertThat(jdbc.queryForObject("select created_by from academic_periods where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.GET, "?q=ganjil&academicYearId=" + yearA, scheduler, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, body(yearA, "REVISI")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, body(yearA, "revisi")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select row_status from academic_periods where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, body(yearA, "REVISI")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void checksSchoolRoleParentAndDateContainment() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, body(yearA, "GANJIL")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, body(yearA, "DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, body(yearA, "DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, body(yearB, "STOLEN")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, body(yearB, "CROSS")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "?academicYearId=" + yearB, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("academicYearId", yearA, "code", "OUTSIDE", "name", "Outside", "startsOn", "2026-06-01", "endsOn", "2026-12-31")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("academicYearId", yearA, "code", "REVERSE", "name", "Reverse", "startsOn", "2026-12-31", "endsOn", "2026-07-01")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?q=%25", scheduler, schoolA, null).getBody().path("result")).isEmpty();
    }

    @Test
    void preventsDeletingOrMovingAReferencedPeriod() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, body(yearA, "USED")).getBody().path("result").path("id").asLong();
        jdbc.update("insert into schedule_versions(school_id, academic_period_id, version_number, name) values (?, ?, 1, 'Draft')", schoolA, id);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        long anotherYear = year(schoolA, "ANOTHER");
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, body(anotherYear, "MOVED")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Existing academic-year mutation also protects this period's containing range.
        HttpHeaders headers = headers(scheduler, schoolA);
        var resize = http.exchange("/v1/academic-years/" + yearA, HttpMethod.PUT, new HttpEntity<>(Map.of("code", "YEAR-A", "name", "Tahun ajaran", "startsOn", "2026-08-01", "endsOn", "2027-06-30", "active", false), headers), JsonNode.class);
        assertThat(resize.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private long school() {
        return jdbc.queryForObject("insert into schools(code,name) values (?, 'Period test school') returning id", Long.class, UUID.randomUUID().toString().substring(0, 20));
    }

    private long year(long schoolId, String code) {
        return jdbc.queryForObject("insert into academic_years(school_id,code,name,starts_on,ends_on) values (?,?,'Tahun ajaran','2026-07-01','2027-06-30') returning id", Long.class, schoolId, code);
    }

    private String identity(String role, long schoolId) {
        long userId = jdbc.queryForObject("insert into users(username,password) values (?, 'not-used-for-login') returning id", Long.class, "period-" + UUID.randomUUID().toString().substring(0, 12));
        long roleId = jdbc.queryForObject("select id from roles where code = ?", Long.class, role);
        long membershipId = jdbc.queryForObject("insert into school_memberships(school_id,user_id) values (?,?) returning id", Long.class, schoolId, userId);
        jdbc.update("insert into school_membership_roles(membership_id,role_id) values (?,?)", membershipId, roleId);
        return jwt.issue(userId);
    }

    private Map<String, Object> body(long yearId, String code) {
        return Map.of("academicYearId", yearId, "code", code, "name", "Semester ganjil", "startsOn", "2026-07-01", "endsOn", "2026-12-31");
    }

    private HttpHeaders headers(String token, long schoolId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-School-ID", String.valueOf(schoolId));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, long schoolId, Object body) {
        return http.exchange("/v1/academic-periods" + suffix, method, new HttpEntity<>(body, headers(token, schoolId)), JsonNode.class);
    }
}
