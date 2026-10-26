package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

@Entity
@Table(name = "time_slots", uniqueConstraints = @UniqueConstraint(name = "uk_time_slot_year_day_sequence", columnNames = {"academic_year_id", "day_of_week", "sequence_number"}))
@Getter @Setter @NoArgsConstructor
public class TimeSlot extends ASoftDeletable {
    public enum Kind { LESSON, BREAK, ACTIVITY, OTHER }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_year_id", nullable = false) private AcademicYear academicYear;
    @Column(name = "day_of_week", nullable = false) private short dayOfWeek;
    @Column(name = "sequence_number", nullable = false) private short sequenceNumber;
    @Column(nullable = false, length = 100) private String label;
    @Column(name = "starts_at", nullable = false) private LocalTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalTime endsAt;
    @Column(name = "duration_minutes", nullable = false) private short durationMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Kind kind = Kind.LESSON;
}
