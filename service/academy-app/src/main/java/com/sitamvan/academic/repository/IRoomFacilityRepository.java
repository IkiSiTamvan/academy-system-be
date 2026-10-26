package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.Facility;
import com.sitamvan.academic.model.RoomFacility;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.*;
import java.util.Collection;
import java.util.List;

public interface IRoomFacilityRepository extends JpaRepository<RoomFacility, Long> {
    @Query("select rf from RoomFacility rf join fetch rf.facility where rf.room.id in :roomIds and rf.room.school.id = :schoolId and rf.facility.school.id = :schoolId and rf.facility.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE order by rf.facility.name, rf.facility.id")
    List<RoomFacility> findForRooms(Collection<Long> roomIds, Long schoolId);

    @Query("select f from Facility f where f.id in :ids and f.school.id = :schoolId and f.rowStatus = :status")
    List<Facility> findLiveFacilities(Collection<Long> ids, Long schoolId, RowStatus status);

    List<RoomFacility> findByRoomId(Long roomId);
}
