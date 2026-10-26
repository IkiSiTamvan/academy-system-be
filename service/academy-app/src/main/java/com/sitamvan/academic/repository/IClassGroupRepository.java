package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.ClassGroup;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface IClassGroupRepository extends JpaRepository<ClassGroup, Long>, JpaSpecificationExecutor<ClassGroup> {
    Optional<ClassGroup> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus status);
    boolean existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, Long yearId, String code, RowStatus status, Long excludedId);

    @Query(value = """
            select exists(select 1 from class_subject_requirements where class_group_id = :classId and row_status = 1)
                or exists(select 1 from schedule_entries se
                    join class_subject_requirements r on r.id = se.requirement_id
                    where r.class_group_id = :classId and se.row_status = 1)
                or exists(select 1 from schedule_entry_slots ses
                    join schedule_entries se on se.id = ses.schedule_entry_id
                    where ses.class_group_id = :classId and se.row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(@Param("classId") Long classId);
}
