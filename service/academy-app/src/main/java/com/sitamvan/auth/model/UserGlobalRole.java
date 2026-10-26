package com.sitamvan.auth.model;

import com.sitamvan.shared.data.util.AAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_global_roles", uniqueConstraints = @UniqueConstraint(name = "uk_user_global_role", columnNames = {"user_id", "role_id"}))
@Getter @Setter @NoArgsConstructor
public class UserGlobalRole extends AAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "role_id", nullable = false) private Role role;
}
