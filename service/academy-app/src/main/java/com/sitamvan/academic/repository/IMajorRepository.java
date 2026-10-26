package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.Major;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface IMajorRepository extends JpaRepository<Major, Long>, JpaSpecificationExecutor<Major> {
    Optional<Major> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);

    boolean existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, String code, RowStatus rowStatus, Long id);

    @Query(value = """
            select exists(select 1 from class_groups where major_id = :id and row_status = 1)
                or exists(select 1 from teacher_majors tm join teachers t on t.id = tm.teacher_id
                    where tm.major_id = :id and t.row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(Long id);
}
