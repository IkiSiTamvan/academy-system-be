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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "academy.bootstrap.platform-admin.username=",
        "academy.bootstrap.platform-admin.password=", "jwt.secret=integration-test-signing-secret-at-least-32-bytes"
})
class TimeSlotIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    long schoolA, schoolB, yearA, yearB;
    String scheduler, viewer, otherScheduler;

    @BeforeEach void fixtures() {
        schoolA = school(); schoolB = school();
        yearA = year(schoolA); yearB = year(schoolB);
        scheduler = identity("SCHEDULER", schoolA);
        viewer = identity("VIEWER", schoolA);
        otherScheduler = identity("SCHEDULER", schoolB);
    }

    @Test void crudDerivesDurationFiltersAuditsAndReusesSoftDeletedSequence() {
        var created = call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:45"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = created.getBody().path("result");
        long id = result.path("id").asLong();
        assertThat(result.path("label").asText()).isEqualTo("Pelajaran 1");
        assertThat(result.path("durationMinutes").asInt()).isEqualTo(45);
        assertThat(result.path("dayName").asText()).isEqualTo("Senin");
        assertThat(result.path("kindLabel").asText()).isEqualTo("Pelajaran");
        assertThat(jdbc.queryForObject("select created_by from time_slots where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.GET, "?academicYearId=" + yearA + "&dayOfWeek=1&q=pelaj&size=1", viewer, schoolA, null).getBody().path("result")).hasSize(1);
        assertThat(call(HttpMethod.GET, "?dayOfWeek=2", viewer, schoolA, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.GET, "?q=%25", viewer, schoolA, null).getBody().path("result")).isEmpty();
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:50")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select duration_minutes from time_slots where id = ?", Integer.class, id)).isEqualTo(50);
        assertThat(jdbc.queryForObject("select modified_by from time_slots where id = ?", Long.class, id)).isPositive();
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select row_status from time_slots where id = ?", Integer.class, id)).isZero();
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void enforcesRolesSchoolScopeAndLiveParentYear() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:45")).getBody().path("result").path("id").asLong();
        assertThat(call(HttpMethod.GET, "", viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", viewer, schoolA, slot(yearA, 1, 2, "08:00", "08:45")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/" + id, viewer, schoolA, slot(yearA, 1, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/" + id, viewer, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "", null, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PUT, "/" + id, otherScheduler, schoolB, slot(yearB, 1, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/" + id, otherScheduler, schoolB, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearB, 1, 1, "08:00", "08:45")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "?academicYearId=" + yearB, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        jdbc.update("update academic_years set row_status = 0 where id = ?", yearA);
        assertThat(call(HttpMethod.GET, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 2, "08:00", "08:45")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        jdbc.update("update schools set row_status = 0 where id = ?", schoolA);
        assertThat(call(HttpMethod.GET, "", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test void rejectsInvalidTimesOverlapsSequenceAndPagingButAllowsAdjacentOrOtherDaySlots() {
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 1, "08:00", "08:45")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 2, "07:30", "08:00")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 2, "06:00", "08:00")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 2, "07:45", "08:30")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 2, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", otherScheduler, schoolB, slot(yearB, 1, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 3, 1, "23:00", "01:00")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 3, 1, "07:00", "07:00")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 3, 1, "07:00:01", "07:45")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 8, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 3, 0, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var invalid = slot(yearA, 3, 1, "07:00", "07:45"); invalid.put("kind", "INVALID");
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        invalid = slot(yearA, 3, 1, "07:00", "07:45"); invalid.put("label", " ");
        assertThat(call(HttpMethod.POST, "", scheduler, schoolA, invalid).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?size=101", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?page=-1", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.GET, "?dayOfWeek=0", scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test void protectsReferencedSlotsWhileAllowingLabelCorrection() {
        long id = call(HttpMethod.POST, "", scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:45")).getBody().path("result").path("id").asLong();
        long teacher = jdbc.queryForObject("insert into teachers(school_id,name) values (?, 'Guru slot') returning id", Long.class, schoolA);
        long period = jdbc.queryForObject("insert into academic_periods(academic_year_id,code,name,starts_on,ends_on) values (?, 'S1','Semester','2026-07-01','2026-12-31') returning id", Long.class, yearA);
        jdbc.update("insert into teacher_availability(teacher_id,academic_period_id,time_slot_id,available) values (?,?,?,true)", teacher, period, id);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, slot(yearA, 1, 1, "07:00", "07:50")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, slot(yearA, 2, 1, "07:00", "07:45")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var labelCorrection = slot(yearA, 1, 1, "07:00", "07:45"); labelCorrection.put("label", "Pelajaran pertama");
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, labelCorrection).getStatusCode()).isEqualTo(HttpStatus.OK);
        jdbc.update("delete from teacher_availability where time_slot_id = ?", id);
        long major = jdbc.queryForObject("insert into majors(school_id,code,name) values (?, 'RPL','RPL') returning id", Long.class, schoolA);
        long group = jdbc.queryForObject("insert into class_groups(school_id,academic_year_id,major_id,code,name,grade_level,cohort_year,section) values (?,?,?,'X-RPL','X RPL',10,2026,'1') returning id", Long.class, schoolA, yearA, major);
        long subject = jdbc.queryForObject("insert into subjects(school_id,code,name) values (?,'MTK','Matematika') returning id", Long.class, schoolA);
        long requirement = jdbc.queryForObject("insert into class_subject_requirements(school_id,class_group_id,academic_period_id,subject_id,sessions_per_week,slots_per_session) values (?,?,?,?,1,1) returning id", Long.class, schoolA, group, period, subject);
        long version = jdbc.queryForObject("insert into schedule_versions(school_id,academic_period_id,version_number,name) values (?,?,1,'Draft') returning id", Long.class, schoolA, period);
        long entry = jdbc.queryForObject("insert into schedule_entries(schedule_version_id,requirement_id,teacher_id,meeting_number) values (?,?,?,1) returning id", Long.class, version, requirement, teacher);
        jdbc.update("insert into schedule_entry_slots(schedule_entry_id,schedule_version_id,class_group_id,teacher_id,time_slot_id,slot_order) values (?,?,?,?,?,1)", entry, version, group, teacher, id);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var kindChange = slot(yearA, 1, 1, "07:00", "07:45"); kindChange.put("kind", "BREAK");
        assertThat(call(HttpMethod.PUT, "/" + id, scheduler, schoolA, kindChange).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbc.update("update schedule_entries set row_status = 0 where id = ?", entry);
        assertThat(call(HttpMethod.DELETE, "/" + id, scheduler, schoolA, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private long school() { return jdbc.queryForObject("insert into schools(code,name) values (?, 'Slot school') returning id", Long.class, UUID.randomUUID().toString().substring(0,20)); }
    private long year(long schoolId) { return jdbc.queryForObject("insert into academic_years(school_id,code,name,starts_on,ends_on,active) values (?, '2026','Tahun 2026','2026-07-01','2027-06-30',false) returning id", Long.class, schoolId); }
    private Map<String,Object> slot(long yearId, int day, int sequence, String starts, String ends) {
        return new HashMap<>(Map.of("academicYearId",yearId,"dayOfWeek",day,"sequenceNumber",sequence,"label"," Pelajaran " + sequence + " ","startsAt",starts,"endsAt",ends,"kind","LESSON"));
    }
    private String identity(String role, long schoolId) {
        String username = "slot-" + UUID.randomUUID().toString().substring(0,12);
        long user = jdbc.queryForObject("insert into users(username,password) values (?,?) returning id",Long.class,username,encoder.encode("test-password-only"));
        Long roleId = jdbc.queryForObject("select id from roles where code=?",Long.class,role);
        long membership = jdbc.queryForObject("insert into school_memberships(school_id,user_id) values (?,?) returning id",Long.class,schoolId,user);
        jdbc.update("insert into school_membership_roles(membership_id,role_id) values (?,?)",membership,roleId);
        var response = http.postForEntity("/v1/auth/login",Map.of("username",username,"password","test-password-only"),JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("result").path("accessToken").asText();
    }
    private ResponseEntity<JsonNode> call(HttpMethod method,String suffix,String token,Long schoolId,Object body) {
        HttpHeaders headers = new HttpHeaders();
        if(token != null) headers.setBearerAuth(token);
        if(schoolId != null) headers.set("X-School-ID",schoolId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/v1/time-slots"+suffix,method,new HttpEntity<>(body,headers),JsonNode.class);
    }
}
