package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestAcademicPeriod;
import com.sitamvan.academic.mapper.AcademicPeriodMapper;
import com.sitamvan.academic.model.AcademicPeriod;
import com.sitamvan.academic.model.AcademicYear;
import com.sitamvan.academic.model.School;
import com.sitamvan.academic.repository.IAcademicPeriodRepository;
import com.sitamvan.academic.repository.IAcademicYearRepository;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.academic.service.IAcademicPeriodService;
import com.sitamvan.academic.specifications.AcademicPeriodSpecifications;
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
public class AcademicPeriodServiceImpl implements IAcademicPeriodService {
    private final IAcademicPeriodRepository periods;
    private final IAcademicYearRepository years;
    private final ISchoolRepository schools;
    private final AcademicPeriodMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<AcademicPeriod> search(String q, Long academicYearId, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (q != null && q.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        if (academicYearId != null) requireYear(academicYearId, schoolId);
        return periods.findAll(Specification.allOf(AcademicPeriodSpecifications.forSchool(schoolId),
                AcademicPeriodSpecifications.forYear(academicYearId), AcademicPeriodSpecifications.search(q)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startsOn", "id")));
    }

    @Override
    public AcademicPeriod getById(Long id) {
        return findPeriod(id, requireSchool(false).getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public AcademicPeriod create(RequestAcademicPeriod request) {
        Long schoolId = requireSchool(true).getId();
        return save(new AcademicPeriod(), request, schoolId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public AcademicPeriod update(Long id, RequestAcademicPeriod request) {
        Long schoolId = requireSchool(true).getId();
        AcademicPeriod period = findPeriod(id, schoolId);
        if (!period.getAcademicYear().getId().equals(request.academicYearId()) && periods.hasDependencies(id)) {
            throw ServiceException.badRequest("Tahun ajaran tidak dapat diganti karena periode sudah digunakan");
        }
        return save(period, request, schoolId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        AcademicPeriod period = findPeriod(id, requireSchool(true).getId());
        if (periods.hasDependencies(id)) {
            throw ServiceException.badRequest("Periode masih digunakan oleh kebutuhan pelajaran, ketersediaan guru, atau jadwal");
        }
        period.setRowStatus(RowStatus.DELETED);
        periods.saveAndFlush(period);
    }

    private School requireSchool(boolean write) {
        Long schoolId = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(schoolId) : schools.findByIdAndRowStatus(schoolId, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private AcademicYear requireYear(Long id, Long schoolId) {
        return years.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Tahun ajaran tidak ditemukan di sekolah ini"));
    }

    private AcademicPeriod findPeriod(Long id, Long schoolId) {
        AcademicPeriod period = periods.findByIdAndAcademicYearSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Periode tidak ditemukan di sekolah ini"));
        if (period.getAcademicYear().getRowStatus() != RowStatus.ACTIVE) {
            throw ServiceException.notFound("Tahun ajaran periode sudah dihapus");
        }
        return period;
    }

    private AcademicPeriod save(AcademicPeriod period, RequestAcademicPeriod request, Long schoolId) {
        AcademicYear year = requireYear(request.academicYearId(), schoolId);
        if (!request.endsOn().isAfter(request.startsOn())) {
            throw ServiceException.badRequest("Tanggal selesai harus setelah tanggal mulai");
        }
        if (request.startsOn().isBefore(year.getStartsOn()) || request.endsOn().isAfter(year.getEndsOn())) {
            throw ServiceException.badRequest("Tanggal periode harus berada dalam rentang tahun ajaran");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (periods.existsByAcademicYearIdAndCodeIgnoreCaseAndRowStatusAndIdNot(year.getId(), code,
                RowStatus.ACTIVE, period.getId() == null ? 0L : period.getId())) {
            throw ServiceException.duplicate("Kode periode sudah dipakai pada tahun ajaran ini");
        }
        mapper.update(request, period);
        period.setAcademicYear(year);
        period.setCode(code);
        period.setName(request.name().trim());
        try { return periods.saveAndFlush(period); }
        catch (DataIntegrityViolationException ex) {
            throw ServiceException.duplicate("Kode periode sudah dipakai pada tahun ajaran ini");
        }
    }
}
