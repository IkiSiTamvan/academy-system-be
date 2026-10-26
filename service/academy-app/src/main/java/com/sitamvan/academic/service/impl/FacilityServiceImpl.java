package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestFacility;
import com.sitamvan.academic.mapper.FacilityMapper;
import com.sitamvan.academic.model.Facility;
import com.sitamvan.academic.model.School;
import com.sitamvan.academic.repository.IFacilityRepository;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.academic.service.IFacilityService;
import com.sitamvan.academic.specifications.FacilitySpecifications;
import com.sitamvan.auth.security.SchoolContext;
import com.sitamvan.shared.data.exception.ServiceException;
import com.sitamvan.shared.data.util.RowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER', 'TEACHER', 'VIEWER')")
public class FacilityServiceImpl implements IFacilityService {
    private final IFacilityRepository facilities;
    private final ISchoolRepository schools;
    private final FacilityMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<Facility> search(String query, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        return facilities.findAll(Specification.allOf(FacilitySpecifications.forSchool(schoolId), FacilitySpecifications.search(query)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name", "id")));
    }

    @Override
    public Facility getById(Long id) { return findFacility(id, requireSchool(false).getId()); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Facility create(RequestFacility request) {
        Facility facility = new Facility();
        facility.setSchool(requireSchool(true));
        return save(facility, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Facility update(Long id, RequestFacility request) {
        return save(findFacility(id, requireSchool(true).getId()), request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        Facility facility = findFacility(id, requireSchool(true).getId());
        if (facilities.hasDependencies(id)) throw ServiceException.badRequest("Fasilitas masih dipakai oleh ruangan");
        facility.setRowStatus(RowStatus.DELETED);
        facilities.saveAndFlush(facility);
    }

    private School requireSchool(boolean write) {
        Long schoolId = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(schoolId) : schools.findByIdAndRowStatus(schoolId, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private Facility findFacility(Long id, Long schoolId) {
        return facilities.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Fasilitas tidak ditemukan di sekolah ini"));
    }

    private Facility save(Facility facility, RequestFacility request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (facilities.existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(
                facility.getSchool().getId(), code, RowStatus.ACTIVE, facility.getId() == null ? 0L : facility.getId())) {
            throw ServiceException.duplicate("Kode fasilitas sudah dipakai di sekolah ini");
        }
        mapper.update(request, facility);
        facility.setCode(code);
        facility.setName(request.name().trim());
        try { return facilities.saveAndFlush(facility); }
        catch (DataIntegrityViolationException ex) { throw ServiceException.duplicate("Kode fasilitas sudah dipakai di sekolah ini"); }
    }
}
