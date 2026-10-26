package com.sitamvan.academic.repository;

import com.sitamvan.academic.model.TimeSlot;
import com.sitamvan.shared.data.util.RowStatus;
import org.springframework.data.jpa.repository.*;
import java.time.LocalTime;
import java.util.Optional;

public interface ITimeSlotRepository extends JpaRepository<TimeSlot, Long>, JpaSpecificationExecutor<TimeSlot> {
    Optional<TimeSlot> findByIdAndSchoolIdAndRowStatus(Long id, Long schoolId, RowStatus rowStatus);

    @Query("""
        select count(t) > 0 from TimeSlot t where t.school.id = :schoolId
        and t.academicYear.id = :yearId and t.dayOfWeek = :day and t.sequenceNumber = :sequence
        and t.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE and t.id <> :id
        """)
    boolean hasSequence(Long schoolId, Long yearId, short day, short sequence, Long id);

    @Query("""
        select count(t) > 0 from TimeSlot t where t.school.id = :schoolId
        and t.academicYear.id = :yearId and t.dayOfWeek = :day
        and t.startsAt < :endsAt and t.endsAt > :startsAt
        and t.rowStatus = com.sitamvan.shared.data.util.RowStatus.ACTIVE and t.id <> :id
        """)
    boolean hasOverlap(Long schoolId, Long yearId, short day, LocalTime startsAt, LocalTime endsAt, Long id);

    @Query(value = """
        select exists(select 1 from teacher_availability where time_slot_id = :id)
          or exists(select 1 from schedule_entry_slots ses join schedule_entries se on se.id = ses.schedule_entry_id
                    where ses.time_slot_id = :id and se.row_status = 1)
        """, nativeQuery = true)
    boolean hasDependencies(Long id);
}
