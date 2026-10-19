package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.AAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_availability", uniqueConstraints = @UniqueConstraint(name = "uk_teacher_availability_period_slot", columnNames = {"teacher_id", "academic_period_id", "time_slot_id"}))
@Getter @Setter @NoArgsConstructor
public class TeacherAvailability extends AAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false) private Teacher teacher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_period_id", nullable = false) private AcademicPeriod academicPeriod;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "time_slot_id", nullable = false) private TimeSlot timeSlot;
    @Column(nullable = false) private boolean available;
    @Column(length = 255) private String note;
}
