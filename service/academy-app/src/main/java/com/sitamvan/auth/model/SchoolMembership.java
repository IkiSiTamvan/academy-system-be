package com.sitamvan.auth.model;

import com.sitamvan.academic.model.School;
import com.sitamvan.shared.data.util.ASoftDeletable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_memberships", uniqueConstraints =
        @UniqueConstraint(name = "uk_membership_school_user", columnNames = {"school_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class SchoolMembership extends ASoftDeletable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean enabled = true;
}
