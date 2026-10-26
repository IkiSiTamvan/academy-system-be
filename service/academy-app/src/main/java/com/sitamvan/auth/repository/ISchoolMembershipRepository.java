package com.sitamvan.auth.repository;

import com.sitamvan.auth.model.SchoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ISchoolMembershipRepository extends JpaRepository<SchoolMembership, Long> {
    @Query("select smr.role.code from SchoolMembershipRole smr where smr.membership.user.id = :userId and smr.membership.school.id = :schoolId and smr.membership.enabled = true and smr.membership.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE")
    List<String> findRoleCodes(@Param("userId") Long userId, @Param("schoolId") Long schoolId);

    @Query("select m from SchoolMembership m where m.user.id = :userId and m.school.id = :schoolId and m.enabled = true and m.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE")
    Optional<SchoolMembership> findActiveMembership(@Param("userId") Long userId, @Param("schoolId") Long schoolId);
    Optional<SchoolMembership> findByUserIdAndSchoolId(Long userId, Long schoolId);
    @Query("select m from SchoolMembership m join fetch m.school where m.user.id = :userId and m.enabled = true and m.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE and m.school.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE")
    List<SchoolMembership> findActiveMembershipsByUserId(@Param("userId") Long userId);
}
