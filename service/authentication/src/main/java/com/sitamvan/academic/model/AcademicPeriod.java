package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "academic_periods", uniqueConstraints = @UniqueConstraint(name = "uk_period_year_code", columnNames = {"academic_year_id", "code"}))
@Getter @Setter @NoArgsConstructor
public class AcademicPeriod extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_year_id", nullable = false) private AcademicYear academicYear;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "starts_on", nullable = false) private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false) private LocalDate endsOn;
}
