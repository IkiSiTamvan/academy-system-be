package com.sitamvan.auth.repository;

import com.sitamvan.auth.model.SchoolInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ISchoolInvitationRepository extends JpaRepository<SchoolInvitation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from SchoolInvitation i join fetch i.school join fetch i.role where i.tokenHash = :tokenHash and i.acceptedAt is null")
    Optional<SchoolInvitation> findActiveForUpdate(@Param("tokenHash") String tokenHash);
}
