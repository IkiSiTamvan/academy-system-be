package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.Facility;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface IFacilityRepository extends JpaRepository<Facility, Long>, JpaSpecificationExecutor<Facility> {
    Optional<Facility> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);
    boolean existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, String code, RowStatus rowStatus, Long id);

    @Query(value = """
            select exists(select 1 from room_facilities rf join rooms r on r.id = rf.room_id
                where rf.facility_id = :id and r.row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(Long id);
}
