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
class SubjectIntegrationTest {
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
    void crudNormalizesAndAuditsThenAllowsReusingDeletedCode() {
        var created = call(HttpMethod.POST, "", scheduler, schoolA,
                Map.of("code", " mtk ", "name", " Matematika ", "category", " Umum "));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long id = created.getBody().path("result").path("id").asLong();
        assertThat(created.getBody().path("result").path("code").asText()).isEqualTo("MTK");
        assertThat(created.getBody().path("result").path("name").asText()).isEqualTo("Matematika");
        assertThat(created.getBody().path("result").path("category").asText()).isEqualTo("Umum");
        assertThat(jdbc.queryForObject("select created_by from subjects where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.GET, "?q=matema&size=1", viewer, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.GET, "?q=%25", viewer, schoolA, null).getBody().path("result")).isEmpty();
        var edited = call(HttpMethod.PUT, "/" + id, scheduler, schoolA, Map.of("code", "MTK", "name", "Matematika revisi"));
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edited.getBody().path("result").path("category").isNull()).isTrue();
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select row_status from subjects where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, subject("MTK")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void enforcesTenantScopePermissionsAndLiveSchool() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, subject("PRIVATE")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, subject("DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, subject("DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", otherScheduler, schoolB, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, subject("PRIVATE")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        jdbc.update("update schools set row_status = 0 where id = ?", schoolA);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsDuplicateCodesAndInvalidInput() {
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, subject("MTK")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, subject("mtk")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", otherScheduler, schoolB, subject("MTK")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", " ", "name", "Blank code")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", "LONG", "name", "Valid", "category", "X".repeat(31))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void preventsDeletingSubjectAssignedToTeacher() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, subject("USED")).getBody().path("result").path("id").asLong();
        long teacherId = jdbc.queryForObject("insert into teachers(school_id, employee_code, name) values (?, 'GURU', 'Guru uji') returning id", Long.class, schoolA);
        jdbc.update("insert into teacher_subjects(teacher_id, subject_id) values (?, ?)", teacherId, id);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private long school() {
        return jdbc.queryForObject("insert into schools(code, name) values (?, 'Subject test school') returning id", Long.class, UUID.randomUUID().toString().substring(0, 20));
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
    private Map<String, Object> subject(String code) { return Map.of("code", code, "name", "Mata pelajaran " + code); }
    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, Long schoolId, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (schoolId != null) headers.set("X-School-ID", schoolId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/subjects" + suffix, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
