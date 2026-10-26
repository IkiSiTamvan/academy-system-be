package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestSubject;
import com.sitamvan.academic.mapper.SubjectMapper;
import com.sitamvan.academic.model.School;
import com.sitamvan.academic.model.Subject;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.academic.repository.ISubjectRepository;
import com.sitamvan.academic.service.ISubjectService;
import com.sitamvan.academic.specifications.SubjectSpecifications;
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
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER', 'TEACHER', 'VIEWER')")
public class SubjectServiceImpl implements ISubjectService {
    private final ISubjectRepository subjects;
    private final ISchoolRepository schools;
    private final SubjectMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<Subject> search(String query, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        return subjects.findAll(Specification.allOf(SubjectSpecifications.forSchool(schoolId), SubjectSpecifications.search(query)),
                PageRequest.of(page, size, Sort.by("name", "id")));
    }

    @Override
    public Subject getById(Long id) { return find(id, requireSchool(false).getId()); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Subject create(RequestSubject request) {
        Subject subject = new Subject();
        subject.setSchool(requireSchool(true));
        return save(subject, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Subject update(Long id, RequestSubject request) {
        return save(find(id, requireSchool(true).getId()), request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        Subject subject = find(id, requireSchool(true).getId());
        if (subjects.hasDependencies(id)) {
            throw ServiceException.badRequest("Mata pelajaran masih dipakai oleh kompetensi guru atau kebutuhan pelajaran kelas");
        }
        subject.setRowStatus(RowStatus.DELETED);
        subjects.saveAndFlush(subject);
    }

    private School requireSchool(boolean write) {
        Long id = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(id) : schools.findByIdAndRowStatus(id, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private Subject find(Long id, Long schoolId) {
        return subjects.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Mata pelajaran tidak ditemukan di sekolah ini"));
    }

    private Subject save(Subject subject, RequestSubject request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (subjects.existsBySchoolIdAndCodeIgnoreCaseAndRowStatusAndIdNot(subject.getSchool().getId(), code,
                RowStatus.ACTIVE, subject.getId() == null ? 0L : subject.getId())) {
            throw ServiceException.duplicate("Kode mata pelajaran sudah dipakai di sekolah ini");
        }
        mapper.update(request, subject);
        subject.setCode(code);
        subject.setName(request.name().trim());
        subject.setCategory(request.category() == null || request.category().isBlank() ? null : request.category().trim());
        try { return subjects.saveAndFlush(subject); }
        catch (DataIntegrityViolationException ex) {
            throw ServiceException.duplicate("Kode mata pelajaran sudah dipakai di sekolah ini");
        }
    }
}
