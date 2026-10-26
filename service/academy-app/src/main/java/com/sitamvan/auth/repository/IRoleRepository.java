package com.sitamvan.auth.repository;

import com.sitamvan.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
}
