package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "class_groups")
@Getter @Setter @NoArgsConstructor
public class ClassGroup extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_year_id", nullable = false) private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "major_id", nullable = false) private Major major;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "grade_level", nullable = false) private short gradeLevel;
    @Column(name = "cohort_year", nullable = false) private int cohortYear;
    @Column(nullable = false, length = 20) private String section;
    @Column private Integer capacity;
}
