package com.sitamvan.auth.repository;

import com.sitamvan.auth.model.SchoolMembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISchoolMembershipRoleRepository extends JpaRepository<SchoolMembershipRole, Long> {
    boolean existsByMembership_IdAndRole_Code(Long membershipId, String roleCode);
}
