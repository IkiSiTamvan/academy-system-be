package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.AAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schedule_entry_slots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_class_slot", columnNames = {"schedule_version_id", "class_group_id", "time_slot_id"}),
                @UniqueConstraint(name = "uk_schedule_teacher_slot", columnNames = {"schedule_version_id", "teacher_id", "time_slot_id"}),
                @UniqueConstraint(name = "uk_schedule_room_slot", columnNames = {"schedule_version_id", "room_id", "time_slot_id"})
        })
@Getter @Setter @NoArgsConstructor
public class ScheduleEntrySlot extends AAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "schedule_entry_id", nullable = false) private ScheduleEntry scheduleEntry;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "schedule_version_id", nullable = false) private ScheduleVersion scheduleVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "class_group_id", nullable = false) private ClassGroup classGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false) private Teacher teacher;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id") private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "time_slot_id", nullable = false) private TimeSlot timeSlot;
    @Column(name = "slot_order", nullable = false) private short slotOrder;
}
