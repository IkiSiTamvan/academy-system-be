package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.Subject;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ISubjectRepository extends JpaRepository<Subject, Long>, JpaSpecificationExecutor<Subject> {
    Optional<Subject> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);

    boolean existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, String code, RowStatus rowStatus, Long id);

    @Query(value = """
            select exists(select 1 from teacher_subjects where subject_id = :id)
                or exists(select 1 from class_subject_requirements where subject_id = :id and row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(Long id);
}
