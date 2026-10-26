package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "academic_years", uniqueConstraints = @UniqueConstraint(name = "uk_academic_year_school_code", columnNames = {"school_id", "code"}))
@Getter @Setter @NoArgsConstructor
public class AcademicYear extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @Column(nullable = false, length = 20) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "starts_on", nullable = false) private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false) private LocalDate endsOn;
    @Column(nullable = false) private boolean active;
}
