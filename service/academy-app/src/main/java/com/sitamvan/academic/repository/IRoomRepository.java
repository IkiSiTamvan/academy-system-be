package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.Room;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface IRoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);
    boolean existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(Long schoolId, String code, RowStatus rowStatus, Long id);

    @Query(value = """
            select exists(select 1 from schedule_entries where room_id = :id and row_status = 1)
                or exists(select 1 from schedule_entry_slots s join schedule_entries e on e.id = s.schedule_entry_id
                    where s.room_id = :id and e.row_status = 1)
            """, nativeQuery = true)
    boolean hasDependencies(Long id);
}
