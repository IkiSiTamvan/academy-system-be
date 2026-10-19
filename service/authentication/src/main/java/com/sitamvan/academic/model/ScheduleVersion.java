package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schedule_versions", uniqueConstraints = @UniqueConstraint(name = "uk_schedule_version_period_number", columnNames = {"academic_period_id", "version_number"}))
@Getter @Setter @NoArgsConstructor
public class ScheduleVersion extends ASoftDeletable {
    public enum Status { DRAFT, PUBLISHED, ARCHIVED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_period_id", nullable = false) private AcademicPeriod academicPeriod;
    @Column(name = "version_number", nullable = false) private int versionNumber;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status = Status.DRAFT;
}
