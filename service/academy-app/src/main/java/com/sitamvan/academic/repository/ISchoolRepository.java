package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import com.sitamvan.shared.data.util.RowStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ISchoolRepository extends JpaRepository<School, Long> {
    List<School> findByRowStatusOrderByNameAsc(RowStatus rowStatus);

    Optional<School> findByIdAndRowStatus(Long id, RowStatus rowStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from School s where s.id = :id and s.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE")
    Optional<School> findActiveForUpdate(Long id);
}
