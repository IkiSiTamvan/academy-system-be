package com.sitamvan.auth.model;

import com.sitamvan.shared.data.util.AAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_membership_roles", uniqueConstraints =
        @UniqueConstraint(name = "uk_membership_role", columnNames = {"membership_id", "role_id"}))
@Getter @Setter @NoArgsConstructor
public class SchoolMembershipRole extends AAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private SchoolMembership membership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
