package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "majors", uniqueConstraints = @UniqueConstraint(name = "uk_major_school_code", columnNames = {"school_id", "code"}))
@Getter @Setter @NoArgsConstructor
public class Major extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 150) private String programName;
    @Column(length = 150) private String fieldName;
}
