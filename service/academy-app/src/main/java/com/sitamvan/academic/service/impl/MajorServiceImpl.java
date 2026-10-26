package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestMajor;
import com.sitamvan.academic.mapper.MajorMapper;
import com.sitamvan.academic.model.Major;
import com.sitamvan.academic.model.School;
import com.sitamvan.academic.repository.IMajorRepository;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.academic.service.IMajorService;
import com.sitamvan.academic.specifications.MajorSpecifications;
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
public class MajorServiceImpl implements IMajorService {
    private final IMajorRepository majors;
    private final ISchoolRepository schools;
    private final MajorMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<Major> search(String query, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        return majors.findAll(Specification.allOf(MajorSpecifications.forSchool(schoolId), MajorSpecifications.search(query)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name", "id")));
    }

    @Override
    public Major getById(Long id) { return findMajor(id, requireSchool(false).getId()); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Major create(RequestMajor request) {
        Major major = new Major();
        major.setSchool(requireSchool(true));
        return save(major, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Major update(Long id, RequestMajor request) {
        return save(findMajor(id, requireSchool(true).getId()), request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        Major major = findMajor(id, requireSchool(true).getId());
        if (majors.hasDependencies(id)) {
            throw ServiceException.badRequest("Jurusan masih dipakai oleh kelas atau kompetensi guru");
        }
        major.setRowStatus(RowStatus.DELETED);
        majors.saveAndFlush(major);
    }

    private School requireSchool(boolean write) {
        Long schoolId = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(schoolId) : schools.findByIdAndRowStatus(schoolId, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private Major findMajor(Long id, Long schoolId) {
        return majors.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Jurusan tidak ditemukan di sekolah ini"));
    }

    private Major save(Major major, RequestMajor request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (majors.existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(
                major.getSchool().getId(), code, RowStatus.ACTIVE, major.getId() == null ? 0L : major.getId())) {
            throw ServiceException.duplicate("Kode jurusan sudah dipakai di sekolah ini");
        }
        mapper.update(request, major);
        major.setCode(code);
        major.setName(request.name().trim());
        major.setProgramName(optionalText(request.programName()));
        major.setFieldName(optionalText(request.fieldName()));
        try { return majors.saveAndFlush(major); }
        catch (DataIntegrityViolationException ex) { throw ServiceException.duplicate("Kode jurusan sudah dipakai di sekolah ini"); }
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
