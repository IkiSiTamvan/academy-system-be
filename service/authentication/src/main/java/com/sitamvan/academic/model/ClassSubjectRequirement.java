package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "class_subject_requirements", uniqueConstraints = @UniqueConstraint(name = "uk_requirement_class_period_subject", columnNames = {"class_group_id", "academic_period_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor
public class ClassSubjectRequirement extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "class_group_id", nullable = false) private ClassGroup classGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_period_id", nullable = false) private AcademicPeriod academicPeriod;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id", nullable = false) private Subject subject;
    @Column(name = "sessions_per_week", nullable = false) private short sessionsPerWeek;
    @Column(name = "slots_per_session", nullable = false) private short slotsPerSession;
    @Column(name = "requires_lab", nullable = false) private boolean requiresLab;
}
