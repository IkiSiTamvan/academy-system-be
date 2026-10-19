package com.sitamvan.academic.model;

import com.sitamvan.auth.model.User;
import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teachers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_school_code", columnNames = {"school_id", "employee_code"}),
        @UniqueConstraint(name = "uk_teacher_school_user", columnNames = {"school_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor
public class Teacher extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "school_id", nullable = false) private School school;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "employee_code", length = 40) private String employeeCode;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 255) private String email;
    @Column(length = 40) private String phone;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private boolean external;
}
