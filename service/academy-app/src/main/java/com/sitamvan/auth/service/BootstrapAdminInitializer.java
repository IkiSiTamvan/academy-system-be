package com.sitamvan.auth.service;

import com.sitamvan.auth.config.BootstrapAdminProperties;
import com.sitamvan.auth.model.Role;
import com.sitamvan.auth.model.User;
import com.sitamvan.auth.model.UserGlobalRole;
import com.sitamvan.auth.repository.IRoleRepository;
import com.sitamvan.auth.repository.IUserGlobalRoleRepository;
import com.sitamvan.auth.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final BootstrapAdminProperties properties;
    private final IUserRepository users;
    private final IRoleRepository roles;
    private final IUserGlobalRoleRepository userRoles;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean configured = properties.username() != null && !properties.username().isBlank();
        if (!configured && (properties.password() == null || properties.password().isBlank())) return;
        if (!configured || properties.password() == null || properties.password().isBlank()) {
            throw new IllegalStateException("Set both BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD");
        }

        User user = users.findByUsernameIgnoreCase(properties.username()).orElseGet(() -> {
            User created = new User();
            created.setUsername(properties.username());
            created.setEmail(properties.email());
            created.setPassword(passwordEncoder.encode(properties.password()));
            created.setEnabled(true);
            return users.save(created);
        });

        Role adminRole = roles.findByCode("PLATFORM_ADMIN")
                .orElseThrow(() -> new IllegalStateException("PLATFORM_ADMIN role was not seeded by Flyway"));
        if (!adminRole.isPlatformRole()) {
            throw new IllegalStateException("PLATFORM_ADMIN must be configured as a global platform role");
        }
        if (!userRoles.existsByUser_IdAndRole_Code(user.getId(), adminRole.getCode())) {
            UserGlobalRole assignment = new UserGlobalRole();
            assignment.setUser(user);
            assignment.setRole(adminRole);
            userRoles.save(assignment);
        }
    }
}
