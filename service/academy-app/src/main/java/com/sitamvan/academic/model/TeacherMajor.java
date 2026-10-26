package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.AAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_majors", uniqueConstraints = @UniqueConstraint(name = "uk_teacher_major", columnNames = {"teacher_id", "major_id"}))
@Getter @Setter @NoArgsConstructor
public class TeacherMajor extends AAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false) private Teacher teacher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "major_id", nullable = false) private Major major;
}
