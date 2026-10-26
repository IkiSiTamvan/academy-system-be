package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.AcademicPeriod;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface IAcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long>, JpaSpecificationExecutor<AcademicPeriod> {
    Optional<AcademicPeriod> findByIdAndAcademicYearSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);
    boolean existsByAcademicYearIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long yearId, String code, RowStatus rowStatus, Long excludedId);

    @Query(value = """
            select exists(select 1 from class_subject_requirements where academic_period_id = :periodId and row_status = 1)
                or exists(select 1 from schedule_versions where academic_period_id = :periodId and row_status = 1)
                or exists(select 1 from teacher_availability where academic_period_id = :periodId)
            """, nativeQuery = true)
    boolean hasDependencies(@Param("periodId") Long periodId);
}
