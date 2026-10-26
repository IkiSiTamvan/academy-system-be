package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.AcademicYear;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IAcademicYearRepository extends JpaRepository<AcademicYear, Long>, JpaSpecificationExecutor<AcademicYear> {
    Optional<AcademicYear> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);

    List<AcademicYear> findBySchoolIdAndRowStatusAndActiveTrue(Long schoolId, RowStatus rowStatus);

    boolean existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, String code, RowStatus rowStatus, Long id);

    @Query(value = """
            select exists(select 1 from academic_periods where academic_year_id = :id and row_status = 1)
                or exists(select 1 from class_groups where academic_year_id = :id and row_status = 1)
                or exists(select 1 from time_slots where academic_year_id = :id and row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(Long id);

    @Query(value = """
            select exists(select 1 from academic_periods
                where academic_year_id = :id and row_status = 1
                and (starts_on < :startsOn or ends_on > :endsOn))
            """, nativeQuery = true)
    boolean hasPeriodsOutsideDates(Long id, LocalDate startsOn, LocalDate endsOn);
}
