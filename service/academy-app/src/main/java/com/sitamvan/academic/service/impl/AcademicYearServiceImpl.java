package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestAcademicYear;
import com.sitamvan.academic.mapper.AcademicYearMapper;
import com.sitamvan.academic.model.AcademicYear;
import com.sitamvan.academic.model.School;
import com.sitamvan.academic.repository.IAcademicYearRepository;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.academic.service.IAcademicYearService;
import com.sitamvan.academic.specifications.AcademicYearSpecifications;
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
public class AcademicYearServiceImpl implements IAcademicYearService {
    private final IAcademicYearRepository years;
    private final ISchoolRepository schools;
    private final AcademicYearMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<AcademicYear> search(String query, Boolean active, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        return years.findAll(Specification.allOf(AcademicYearSpecifications.forSchool(schoolId),
                        AcademicYearSpecifications.search(query), AcademicYearSpecifications.active(active)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startsOn", "id")));
    }

    @Override
    public AcademicYear getById(Long id) {
        return findYear(id, requireSchool(false).getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public AcademicYear create(RequestAcademicYear request) {
        School school = requireSchool(true);
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        return save(year, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public AcademicYear update(Long id, RequestAcademicYear request) {
        School school = requireSchool(true);
        AcademicYear year = findYear(id, school.getId());
        if (years.hasPeriodsOutsideDates(id, request.startsOn(), request.endsOn())) {
            throw ServiceException.badRequest("Rentang tanggal harus mencakup seluruh periode akademik yang sudah dibuat");
        }
        return save(year, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        AcademicYear year = findYear(id, requireSchool(true).getId());
        if (year.isActive()) throw ServiceException.badRequest("Nonaktifkan tahun ajaran sebelum menghapusnya");
        if (years.hasDependencies(id)) {
            throw ServiceException.badRequest("Tahun ajaran masih dipakai oleh periode, kelas, atau slot waktu");
        }
        year.setRowStatus(RowStatus.DELETED);
        years.saveAndFlush(year);
    }

    private School requireSchool(boolean write) {
        Long schoolId = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(schoolId) : schools.findByIdAndRowStatus(schoolId, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private AcademicYear findYear(Long id, Long schoolId) {
        return years.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Tahun ajaran tidak ditemukan di sekolah ini"));
    }

    private AcademicYear save(AcademicYear year, RequestAcademicYear request) {
        if (!request.endsOn().isAfter(request.startsOn())) {
            throw ServiceException.badRequest("Tanggal selesai harus setelah tanggal mulai");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (years.existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(
                year.getSchool().getId(), code, RowStatus.ACTIVE, year.getId() == null ? 0L : year.getId())) {
            throw ServiceException.duplicate("Kode tahun ajaran sudah dipakai di sekolah ini");
        }
        // All writes lock the school row, serializing active-year changes per tenant.
        // Flush the previous active year first so the partial unique index stays valid.
        if (request.active()) {
            years.findBySchoolIdAndRowStatusAndActiveTrue(year.getSchool().getId(), RowStatus.ACTIVE)
                    .stream().filter(other -> !other.getId().equals(year.getId()))
                    .forEach(other -> other.setActive(false));
            years.flush();
        }
        mapper.update(request, year);
        year.setCode(code);
        year.setName(request.name().trim());
        try { return years.saveAndFlush(year); }
        catch (DataIntegrityViolationException ex) {
            throw ServiceException.duplicate("Kode atau status aktif tahun ajaran bertabrakan dengan data lain; muat ulang lalu coba lagi");
        }
    }
}
