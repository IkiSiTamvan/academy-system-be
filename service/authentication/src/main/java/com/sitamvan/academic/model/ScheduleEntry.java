package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schedule_entries", uniqueConstraints = @UniqueConstraint(name = "uk_schedule_entry_requirement_meeting", columnNames = {"schedule_version_id", "requirement_id", "meeting_number"}))
@Getter @Setter @NoArgsConstructor
public class ScheduleEntry extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "schedule_version_id", nullable = false) private ScheduleVersion scheduleVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requirement_id", nullable = false) private ClassSubjectRequirement requirement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false) private Teacher teacher;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id") private Room room;
    @Column(name = "meeting_number", nullable = false) private short meetingNumber;
}
