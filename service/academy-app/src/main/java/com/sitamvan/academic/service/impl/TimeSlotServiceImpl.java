package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestTimeSlot;
import com.sitamvan.academic.mapper.TimeSlotMapper;
import com.sitamvan.academic.model.*;
import com.sitamvan.academic.repository.*;
import com.sitamvan.academic.service.ITimeSlotService;
import com.sitamvan.academic.specifications.TimeSlotSpecifications;
import com.sitamvan.auth.security.SchoolContext;
import com.sitamvan.shared.data.exception.ServiceException;
import com.sitamvan.shared.data.util.RowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER', 'TEACHER', 'VIEWER')")
public class TimeSlotServiceImpl implements ITimeSlotService {
    private final ITimeSlotRepository slots;
    private final IAcademicYearRepository years;
    private final ISchoolRepository schools;
    private final TimeSlotMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<TimeSlot> search(String query, Long academicYearId, Short dayOfWeek, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)
                || (academicYearId != null && academicYearId <= 0) || (dayOfWeek != null && (dayOfWeek < 1 || dayOfWeek > 7))) {
            throw ServiceException.badRequest("Parameter pencarian slot waktu tidak valid");
        }
        if (academicYearId != null) requireYear(academicYearId, schoolId);
        return slots.findAll(Specification.allOf(TimeSlotSpecifications.forSchool(schoolId), TimeSlotSpecifications.search(query),
                TimeSlotSpecifications.forYear(academicYearId), TimeSlotSpecifications.forDay(dayOfWeek)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "academicYear.id", "dayOfWeek", "sequenceNumber", "id")));
    }
    @Override
    public TimeSlot getById(Long id) { return findSlot(id, requireSchool(false).getId()); }
    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public TimeSlot create(RequestTimeSlot request) {
        TimeSlot slot = new TimeSlot();
        slot.setSchool(requireSchool(true));
        return save(slot, request);
    }
    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public TimeSlot update(Long id, RequestTimeSlot request) {
        TimeSlot slot = findSlot(id, requireSchool(true).getId());
        boolean structuralChange = !slot.getAcademicYear().getId().equals(request.academicYearId())
                || slot.getDayOfWeek() != request.dayOfWeek() || slot.getSequenceNumber() != request.sequenceNumber()
                || !slot.getStartsAt().equals(request.startsAt()) || !slot.getEndsAt().equals(request.endsAt())
                || slot.getKind() != request.kind();
        if (structuralChange && slots.hasDependencies(id)) {
            throw ServiceException.badRequest("Slot waktu sudah dipakai oleh ketersediaan guru atau jadwal; hanya label yang bisa diubah");
        }
        return save(slot, request);
    }
    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        TimeSlot slot = findSlot(id, requireSchool(true).getId());
        if (slots.hasDependencies(id)) throw ServiceException.badRequest("Slot waktu masih dipakai oleh ketersediaan guru atau jadwal");
        slot.setRowStatus(RowStatus.DELETED);
        slots.saveAndFlush(slot);
    }

    private School requireSchool(boolean write) {
        Long id = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(id) : schools.findByIdAndRowStatus(id, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }
    private AcademicYear requireYear(Long id, Long schoolId) {
        return years.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Tahun ajaran tidak ditemukan di sekolah ini"));
    }
    private TimeSlot findSlot(Long id, Long schoolId) {
        return slots.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .filter(slot -> slot.getAcademicYear().getRowStatus() == RowStatus.ACTIVE
                        && slot.getAcademicYear().getSchool().getId().equals(schoolId))
                .orElseThrow(() -> ServiceException.notFound("Slot waktu tidak ditemukan di sekolah ini"));
    }
    private boolean wholeMinute(LocalTime time) { return time.getSecond() == 0 && time.getNano() == 0; }
    private TimeSlot save(TimeSlot slot, RequestTimeSlot request) {
        AcademicYear year = requireYear(request.academicYearId(), slot.getSchool().getId());
        if (!wholeMinute(request.startsAt()) || !wholeMinute(request.endsAt()) || !request.endsAt().isAfter(request.startsAt())) {
            throw ServiceException.badRequest("Gunakan ketelitian menit; jam selesai harus setelah jam mulai pada hari yang sama");
        }
        Long id = slot.getId() == null ? 0L : slot.getId();
        if (slots.hasSequence(slot.getSchool().getId(), year.getId(), request.dayOfWeek(), request.sequenceNumber(), id)) {
            throw ServiceException.duplicate("Nomor urut slot sudah dipakai pada tahun ajaran dan hari ini");
        }
        if (slots.hasOverlap(slot.getSchool().getId(), year.getId(), request.dayOfWeek(), request.startsAt(), request.endsAt(), id)) {
            throw ServiceException.badRequest("Rentang jam bertabrakan dengan slot waktu lain pada hari yang sama");
        }
        mapper.update(request, slot);
        slot.setAcademicYear(year);
        slot.setLabel(request.label().trim());
        slot.setDurationMinutes((short) Duration.between(request.startsAt(), request.endsAt()).toMinutes());
        try { return slots.saveAndFlush(slot); }
        catch (DataIntegrityViolationException ex) { throw ServiceException.duplicate("Nomor urut slot sudah dipakai pada tahun ajaran dan hari ini"); }
    }
}
