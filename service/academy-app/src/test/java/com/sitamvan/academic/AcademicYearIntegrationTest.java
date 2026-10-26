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
class AcademicYearIntegrationTest {
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
    void createsSearchesUpdatesAndSoftDeletesWithoutLosingAuditHistory() {
        var created = call(HttpMethod.POST, "", scheduler, schoolA, year("2026-2027", false));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long id = created.getBody().path("result").path("id").asLong();
        assertThat(created.getBody().path("result").path("startsOn").asText()).isEqualTo("2026-07-01");
        assertThat(created.getBody().path("result").path("createdAt").asText()).isNotBlank();
        assertThat(jdbc.queryForObject("select created_by from academic_years where id = ?", Long.class, id)).isPositive();

        var listed = call(HttpMethod.GET, "?q=2026&page=0&size=10", scheduler, schoolA, null);
        assertThat(listed.getBody().path("result")).hasSize(1);
        assertThat(listed.getBody().path("metaData").path("totalData").asInt()).isEqualTo(1);
        var edited = call(HttpMethod.PUT, "/" + id, scheduler, schoolA,
                Map.of("code", "  edited  ", "name", "  Tahun ajaran revisi  ", "startsOn", "2026-07-01", "endsOn", "2027-06-30", "active", false));
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edited.getBody().path("result").path("code").asText()).isEqualTo("EDITED");
        assertThat(edited.getBody().path("result").path("name").asText()).isEqualTo("Tahun ajaran revisi");
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("select row_status from academic_years where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, year("EDITED", false)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void enforcesMembershipAndRolePermissionsOnTheServer() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, year("SECURE", false)).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, year("DENIED", false)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, year("DENIED", false)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", scheduler, null, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void scopesEveryLookupAndMutationToTheCurrentSchool() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, year("PRIVATE", false)).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", otherScheduler, schoolB, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, year("STOLEN", false)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidDatesMalformedInputAndUnboundedPages() {
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("code", "BAD", "name", "Invalid range", "startsOn", "2027-06-30", "endsOn", "2026-07-01", "active", false)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("code", " ", "name", "Invalid", "startsOn", "not-a-date", "endsOn", "2027-06-30", "active", false)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", "MISSING")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?active=not-a-boolean", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void codesAreUniquePerSchoolAndCaseInsensitive() {
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, year("DUPLICATE", false)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, year("duplicate", false)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", otherScheduler, schoolB, year("DUPLICATE", false)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "?q=%25", scheduler, schoolA, null).getBody().path("result")).isEmpty();
    }

    @Test
    void activatingAnotherYearDeactivatesThePreviousOneAndProtectsActiveDeletion() {
        long first = call(HttpMethod.POST, "", scheduler, schoolA, year("FIRST", true)).getBody().path("result").path("id").asLong();
        long second = call(HttpMethod.POST, "", scheduler, schoolA, year("SECOND", true)).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "/" + first, scheduler, schoolA, null).getBody().path("result").path("active").asBoolean()).isFalse();
        assertThat(call(HttpMethod.GET, "?active=true", scheduler, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from academic_years where school_id = ? and active and row_status = 1", Long.class, schoolA)).isEqualTo(1);
        assertThat(call(HttpMethod.DELETE, "/" + second, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + second, scheduler, schoolA, year("SECOND", false)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.DELETE, "/" + second, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void preservesExistingPeriodsWhenResizingOrDeletingAYear() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, year("USED", false)).getBody().path("result").path("id").asLong();
        jdbc.update("insert into academic_periods(academic_year_id, code, name, starts_on, ends_on) values (?, 'GANJIL', 'Ganjil', '2026-07-01', '2026-12-31')", id);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA,
                Map.of("code", "USED", "name", "Used", "startsOn", "2026-08-01", "endsOn", "2027-06-30", "active", false)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void schoolPickerReturnsOnlyAccessibleSchoolsExceptForPlatformAdministrators() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(scheduler);
        var response = http.exchange("/v1/schools", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("result")).hasSize(1);
        assertThat(response.getBody().path("result").get(0).path("id").asLong()).isEqualTo(schoolA);
        headers.setBearerAuth(identity("PLATFORM_ADMIN", schoolA));
        var admin = http.exchange("/v1/schools", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(admin.getBody().path("result").size()).isGreaterThanOrEqualTo(3);
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

    private Map<String, Object> year(String code, boolean active) {
        return Map.of("code", code, "name", "Tahun ajaran " + code,
                "startsOn", "2026-07-01", "endsOn", "2027-06-30", "active", active);
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, Long schoolId, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (schoolId != null) headers.set("X-School-ID", schoolId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/academic-years" + suffix, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
