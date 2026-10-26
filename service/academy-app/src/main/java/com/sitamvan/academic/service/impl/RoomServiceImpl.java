package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestRoom;
import com.sitamvan.academic.dto.response.ResponseRoom;
import com.sitamvan.academic.mapper.RoomMapper;
import com.sitamvan.academic.model.*;
import com.sitamvan.academic.repository.*;
import com.sitamvan.academic.service.IRoomService;
import com.sitamvan.academic.specifications.RoomSpecifications;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER', 'TEACHER', 'VIEWER')")
public class RoomServiceImpl implements IRoomService {
    private final IRoomRepository rooms;
    private final IRoomFacilityRepository roomFacilities;
    private final ISchoolRepository schools;
    private final RoomMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<ResponseRoom> search(String query, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        Page<Room> result = rooms.findAll(Specification.allOf(RoomSpecifications.forSchool(schoolId), RoomSpecifications.search(query)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name", "id")));
        Map<Long, List<Facility>> facilities = result.isEmpty() ? Map.of() : roomFacilities.findForRooms(result.map(Room::getId).getContent(), schoolId).stream()
                .collect(Collectors.groupingBy(link -> link.getRoom().getId(), Collectors.mapping(RoomFacility::getFacility, Collectors.toList())));
        return result.map(room -> mapper.toResponse(room, facilities.getOrDefault(room.getId(), List.of())));
    }

    @Override
    public ResponseRoom getById(Long id) { return response(findRoom(id, requireSchool(false).getId())); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public ResponseRoom create(RequestRoom request) {
        Room room = new Room();
        room.setSchool(requireSchool(true));
        return save(room, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public ResponseRoom update(Long id, RequestRoom request) { return save(findRoom(id, requireSchool(true).getId()), request); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        Room room = findRoom(id, requireSchool(true).getId());
        if (rooms.hasDependencies(id)) throw ServiceException.badRequest("Ruangan masih dipakai oleh jadwal");
        room.setRowStatus(RowStatus.DELETED);
        rooms.saveAndFlush(room);
    }

    private School requireSchool(boolean write) {
        Long schoolId = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(schoolId) : schools.findByIdAndRowStatus(schoolId, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private Room findRoom(Long id, Long schoolId) {
        return rooms.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Ruangan tidak ditemukan di sekolah ini"));
    }

    private ResponseRoom response(Room room) {
        return mapper.toResponse(room, roomFacilities.findForRooms(List.of(room.getId()), room.getSchool().getId()).stream().map(RoomFacility::getFacility).toList());
    }

    private ResponseRoom save(Room room, RequestRoom request) {
        Long schoolId = room.getSchool().getId();
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String roomType = request.roomType().trim().toUpperCase(Locale.ROOT);
        if (rooms.existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(schoolId, code, RowStatus.ACTIVE, room.getId() == null ? 0L : room.getId())) {
            throw ServiceException.duplicate("Kode ruangan sudah dipakai di sekolah ini");
        }
        Set<Long> wantedIds = new LinkedHashSet<>(request.facilityIds());
        if (wantedIds.size() != request.facilityIds().size()) throw ServiceException.badRequest("Daftar fasilitas tidak boleh berulang");
        List<Facility> wanted = wantedIds.isEmpty() ? List.of() : roomFacilities.findLiveFacilities(wantedIds, schoolId, RowStatus.ACTIVE);
        if (wanted.size() != wantedIds.size()) throw ServiceException.badRequest("Fasilitas harus aktif dan berasal dari sekolah yang sama");
        List<RoomFacility> existing = room.getId() == null ? List.of() : roomFacilities.findByRoomId(room.getId());
        Set<Long> existingIds = existing.stream().map(link -> link.getFacility().getId()).collect(Collectors.toSet());
        if (room.getId() != null && rooms.hasDependencies(room.getId()) &&
                (!Objects.equals(room.getCapacity(), request.capacity()) || !room.getRoomType().equals(roomType) || !existingIds.equals(wantedIds))) {
            throw ServiceException.badRequest("Kapasitas, jenis, dan fasilitas ruangan yang dipakai jadwal tidak dapat diubah");
        }
        mapper.update(request, room);
        room.setCode(code);
        room.setName(request.name().trim());
        room.setRoomType(roomType);
        try { rooms.saveAndFlush(room); }
        catch (DataIntegrityViolationException ex) { throw ServiceException.duplicate("Kode ruangan sudah dipakai di sekolah ini"); }
        roomFacilities.deleteAll(existing.stream().filter(link -> !wantedIds.contains(link.getFacility().getId())).toList());
        for (Facility facility : wanted) {
            if (!existingIds.contains(facility.getId())) {
                RoomFacility link = new RoomFacility();
                link.setRoom(room);
                link.setFacility(facility);
                roomFacilities.save(link);
            }
        }
        roomFacilities.flush();
        return response(room);
    }
}
