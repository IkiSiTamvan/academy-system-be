package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.Teacher;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ITeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {
    Optional<Teacher> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);
    boolean existsBySchoolIdAndEmployeeCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, String employeeCode, RowStatus rowStatus, Long id);

    @Query(value = """
            select exists(select 1 from teacher_subjects where teacher_id = :id)
                or exists(select 1 from teacher_majors where teacher_id = :id)
                or exists(select 1 from teacher_availability where teacher_id = :id)
                or exists(select 1 from schedule_entries where teacher_id = :id and row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(Long id);
}
