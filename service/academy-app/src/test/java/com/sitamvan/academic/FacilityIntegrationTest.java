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
class FacilityIntegrationTest {
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
    void createsSearchesUpdatesAndSoftDeletesWithAuditAndCodeReuse() {
        var created = call(HttpMethod.POST, "", scheduler, schoolA, facility("  projector  "));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        var result = created.getBody().path("result");
        long id = result.path("id").asLong();
        assertThat(result.path("code").asText()).isEqualTo("PROJECTOR");
        assertThat(result.path("createdAt").asText()).isNotBlank();
        assertThat(result.has("school")).isFalse();
        assertThat(jdbc.queryForObject("select created_by from facilities where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.GET, "?q=projector", scheduler, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.GET, "?q=%25", scheduler, schoolA, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "?q=_", scheduler, schoolA, null).getBody().path("result")).isEmpty();
        var updated = call(HttpMethod.PUT, "/" + id, scheduler, schoolA, Map.of("code", "screen", "name", "  Layar presentasi  "));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("result").path("name").asText()).isEqualTo("Layar presentasi");
        assertThat(updated.getBody().path("result").path("modifiedAt").asText()).isNotBlank();
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolA, null).getBody().path("result")).isEmpty();
        assertThat(jdbc.queryForObject("select row_status from facilities where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, facility("screen")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void validatesInputAndScopesDuplicateCodesPerSchool() {
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, facility("PC")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, facility("pc")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", otherScheduler, schoolB, facility("PC")).getStatusCode()).isEqualTo(HttpStatus.OK);
        long otherId = call(HttpMethod.POST, "", scheduler, schoolA, facility("OTHER")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.PUT, "/" + otherId, scheduler, schoolA, facility("pc")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", " ", "name", "Fasilitas")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", "X", "name", "x".repeat(121))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", "X".repeat(31), "name", "Fasilitas")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, Map.of("code", "MISSING")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=0", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=abc", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?q=" + "x".repeat(101), scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void enforcesSchoolAndRoleBoundaries() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, facility("PRIVATE")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, facility("DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, facility("DENIED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", otherScheduler, schoolB, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, facility("STOLEN")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        jdbc.update("update schools set row_status = 0 where id = ?", schoolA);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void preventsDeletingFacilitiesInUseByLiveRooms() {
        long facilityId = call(HttpMethod.POST, "", scheduler, schoolA, facility("USED")).getBody().path("result").path("id").asLong();
        long roomId = jdbc.queryForObject("insert into rooms(school_id, code, name, room_type) values (?, 'LAB', 'Laboratorium', 'LAB') returning id", Long.class, schoolA);
        jdbc.update("insert into room_facilities(room_id, facility_id) values (?, ?)", roomId, facilityId);
        assertThat(call(HttpMethod.DELETE, "/" + facilityId, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("update rooms set row_status = 0 where id = ?", roomId);
        assertThat(call(HttpMethod.DELETE, "/" + facilityId, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
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

    private Map<String, Object> facility(String code) {
        return Map.of("code", code, "name", "Fasilitas " + code);
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, Long schoolId, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        if (schoolId != null) headers.set("X-School-ID", schoolId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/facilities" + suffix, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}