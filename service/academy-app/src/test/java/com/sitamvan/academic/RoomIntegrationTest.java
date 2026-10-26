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
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "academy.bootstrap.platform-admin.username=", "academy.bootstrap.platform-admin.password=",
        "jwt.secret=integration-test-signing-secret-at-least-32-bytes"
})
class RoomIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    long schoolA, schoolB;
    String scheduler, viewer, otherScheduler;

    @BeforeEach void fixtures() {
        schoolA = school(); schoolB = school();
        scheduler = identity("SCHEDULER", schoolA); viewer = identity("VIEWER", schoolA); otherScheduler = identity("SCHEDULER", schoolB);
    }

    @Test void persistsRoomAndReplacesFacilityAssignmentsWithoutDroppingRetainedAuditRows() {
        long computer = facility(schoolA, "Komputer");
        long projector = facility(schoolA, "Proyektor");
        var created = call(HttpMethod.POST, "", scheduler, schoolA, room(" lab-1 ", List.of(computer)));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        var item = created.getBody().path("result"); long id = item.path("id").asLong();
        assertThat(item.path("code").asText()).isEqualTo("LAB-1");
        assertThat(item.path("roomType").asText()).isEqualTo("LAB");
        assertThat(item.path("facilityIds").get(0).asLong()).isEqualTo(computer);
        assertThat(item.path("facilityNames").get(0).asText()).isEqualTo("Komputer");
        assertThat(item.path("createdAt").asText()).isNotBlank();
        assertThat(jdbc.queryForObject("select created_by from rooms where id = ?", Long.class, id)).isPositive();
        long link = jdbc.queryForObject("select id from room_facilities where room_id = ?", Long.class, id);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, room("LAB-1", List.of(computer, projector))).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select id from room_facilities where room_id = ? and facility_id = ?", Long.class, id, computer)).isEqualTo(link);
        assertThat(call(HttpMethod.GET, "?q=lab&size=10", scheduler, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.GET, "?q=%25", scheduler, schoolA, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, room("LAB-1", List.of(projector))).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select count(*) from room_facilities where room_id = ?", Long.class, id)).isEqualTo(1);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getBody().path("result").path("facilityIds").get(0).asLong()).isEqualTo(projector);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("select row_status from rooms where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, room("lab-1", List.of())).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void checksRolesTenantIsolationAndLiveSchool() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, room("PRIVATE", List.of())).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
            assertThat(call(method, method.equals(HttpMethod.POST) ? "" : "/" + id, viewer, schoolA, room("DENIED", List.of())).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(call(HttpMethod.GET, "", scheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)) {
            assertThat(call(method, "/" + id, otherScheduler, schoolB, room("STOLEN", List.of())).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
        assertThat(call(HttpMethod.GET, "", otherScheduler, schoolB, null).getBody().path("result")).isEmpty();
        jdbc.update("update schools set row_status = 0 where id = ?", schoolA);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test void validatesCodesCapacityAndSameSchoolLiveFacilities() {
        long local = facility(schoolA, "Fasilitas lokal"); long foreign = facility(schoolB, "Fasilitas sekolah lain");
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, room("ROOM", List.of())).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, room("room", List.of())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", otherScheduler, schoolB, room("ROOM", List.of())).getStatusCode()).isEqualTo(HttpStatus.OK);
        for (List<Long> ids : List.of(List.of(foreign), List.of(Long.MAX_VALUE), List.of(local, local), List.of(-1L))) {
            assertThat(call(HttpMethod.POST, "", scheduler, schoolA, room("INVALID", ids)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        jdbc.update("update facilities set row_status = 0 where id = ?", local);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, room("INVALID", List.of(local))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String,Object> invalid = new HashMap<>(room("INVALID", List.of()));
        invalid.put("capacity", 0);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        invalid.put("capacity", null); invalid.put("name", "x".repeat(121));
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        invalid.put("name", "Valid"); invalid.put("facilityIds", null);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test void protectsScheduledRoomsAndAllowsDescriptiveEdits() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, room("USED", List.of())).getBody().path("result").path("id").asLong();
        long entry = scheduledRoom(id);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String,Object> changed = new HashMap<>(room("RENAMED", List.of())); changed.put("capacity", 40);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, changed).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, room("RENAMED", List.of())).getStatusCode()).isEqualTo(HttpStatus.OK);
        long slotRoom = call(HttpMethod.POST, "", scheduler, schoolA, room("SLOT-ROOM", List.of())).getBody().path("result").path("id").asLong();
        jdbc.update("update schedule_entry_slots set room_id = ? where schedule_entry_id = ?", slotRoom, entry);
        assertThat(call(HttpMethod.DELETE, "/" + slotRoom, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("update schedule_entries set row_status = 0 where id = ?", entry);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.DELETE, "/" + slotRoom, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private long scheduledRoom(long roomId) {
        long year = jdbc.queryForObject("insert into academic_years(school_id, code, name, starts_on, ends_on) values (?, 'YEAR', 'Year', '2026-07-01', '2027-06-30') returning id", Long.class, schoolA);
        long period = jdbc.queryForObject("insert into academic_periods(academic_year_id, code, name, starts_on, ends_on) values (?, 'S1', 'Semester', '2026-07-01', '2026-12-31') returning id", Long.class, year);
        long major = jdbc.queryForObject("insert into majors(school_id, code, name) values (?, 'RPL', 'RPL') returning id", Long.class, schoolA);
        long group = jdbc.queryForObject("insert into class_groups(school_id, academic_year_id, major_id, code, name, grade_level, cohort_year, section) values (?, ?, ?, 'X', 'X', 10, 2026, '1') returning id", Long.class, schoolA, year, major);
        long subject = jdbc.queryForObject("insert into subjects(school_id, code, name) values (?, 'MTK', 'Matematika') returning id", Long.class, schoolA);
        long requirement = jdbc.queryForObject("insert into class_subject_requirements(school_id, class_group_id, academic_period_id, subject_id, sessions_per_week, slots_per_session) values (?, ?, ?, ?, 1, 1) returning id", Long.class, schoolA, group, period, subject);
        long teacher = jdbc.queryForObject("insert into teachers(school_id, name) values (?, 'Guru') returning id", Long.class, schoolA);
        long version = jdbc.queryForObject("insert into schedule_versions(school_id, academic_period_id, version_number, name) values (?, ?, 1, 'Draft') returning id", Long.class, schoolA, period);
        long entry = jdbc.queryForObject("insert into schedule_entries(schedule_version_id, requirement_id, teacher_id, room_id, meeting_number) values (?, ?, ?, ?, 1) returning id", Long.class, version, requirement, teacher, roomId);
        long slot = jdbc.queryForObject("insert into time_slots(school_id, academic_year_id, day_of_week, sequence_number, label, starts_at, ends_at, duration_minutes, kind) values (?, ?, 1, 1, 'Jam 1', '07:00', '07:45', 45, 'LESSON') returning id", Long.class, schoolA, year);
        jdbc.update("insert into schedule_entry_slots(schedule_entry_id, schedule_version_id, class_group_id, teacher_id, room_id, time_slot_id, slot_order) values (?, ?, ?, ?, ?, ?, 1)", entry, version, group, teacher, roomId, slot);
        return entry;
    }
    private long facility(long schoolId, String name) { return jdbc.queryForObject("insert into facilities(school_id, code, name) values (?, ?, ?) returning id", Long.class, schoolId, UUID.randomUUID().toString().substring(0,20), name); }
    private long school() { return jdbc.queryForObject("insert into schools(code, name) values (?, 'Integration school') returning id", Long.class, UUID.randomUUID().toString().substring(0,20)); }
    private Map<String,Object> room(String code, List<Long> facilities) { return Map.of("code",code,"name","Ruangan " + code,"roomType"," lab ","capacity",36,"facilityIds",facilities); }
    private String identity(String role, long schoolId) {
        String username = "test-" + UUID.randomUUID().toString().substring(0,12);
        long user = jdbc.queryForObject("insert into users(username, password) values (?, ?) returning id", Long.class, username, encoder.encode("test-password-only"));
        long membership = jdbc.queryForObject("insert into school_memberships(school_id, user_id) values (?, ?) returning id", Long.class, schoolId, user);
        jdbc.update("insert into school_membership_roles(membership_id, role_id) values (?, (select id from roles where code = ?))", membership, role);
        var response = http.postForEntity("/v1/auth/login", Map.of("username",username,"password","test-password-only"),JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("result").path("accessToken").asText();
    }
    private ResponseEntity<JsonNode> call(HttpMethod method, String suffix, String token, Long schoolId, Object body) {
        HttpHeaders headers = new HttpHeaders(); if (token != null) headers.setBearerAuth(token); if (schoolId != null) headers.set("X-School-ID", schoolId.toString()); headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/rooms" + suffix, method, new HttpEntity<>(body,headers),JsonNode.class);
    }
}
