package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schools", uniqueConstraints = @UniqueConstraint(name = "uk_school_code", columnNames = "code"))
@Getter @Setter @NoArgsConstructor
public class School extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 500) private String address;
    @Column(name = "time_zone", nullable = false, length = 50) private String timeZone = "Asia/Jakarta";
}
