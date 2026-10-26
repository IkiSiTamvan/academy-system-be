package com.sitamvan.auth.repository;

import com.sitamvan.auth.model.UserGlobalRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface IUserGlobalRoleRepository extends JpaRepository<UserGlobalRole, Long> {
    @Query("select ugr.role.code from UserGlobalRole ugr where ugr.user.id = :userId")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
    boolean existsByUser_IdAndRole_Code(Long userId, String roleCode);
}
