package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestTeacher;
import com.sitamvan.academic.mapper.TeacherMapper;
import com.sitamvan.academic.model.School;
import com.sitamvan.academic.model.Teacher;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.academic.repository.ITeacherRepository;
import com.sitamvan.academic.service.ITeacherService;
import com.sitamvan.academic.specifications.TeacherSpecifications;
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
public class TeacherServiceImpl implements ITeacherService {
    private final ITeacherRepository teachers;
    private final ISchoolRepository schools;
    private final TeacherMapper mapper;
    private final SchoolContext schoolContext;

    @Override
    public Page<Teacher> search(String query, Boolean active, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1-100");
        }
        return teachers.findAll(Specification.allOf(TeacherSpecifications.forSchool(schoolId), TeacherSpecifications.search(query), TeacherSpecifications.active(active)),
                PageRequest.of(page, size, Sort.by("name", "id")));
    }

    @Override
    public Teacher getById(Long id) { return find(id, requireSchool(false).getId()); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Teacher create(RequestTeacher request) {
        Teacher teacher = new Teacher();
        teacher.setSchool(requireSchool(true));
        return save(teacher, request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public Teacher update(Long id, RequestTeacher request) {
        return save(find(id, requireSchool(true).getId()), request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        Teacher teacher = find(id, requireSchool(true).getId());
        if (teachers.hasDependencies(id)) {
            throw ServiceException.badRequest("Guru masih dipakai oleh kompetensi, ketersediaan, atau jadwal pelajaran");
        }
        teacher.setRowStatus(RowStatus.DELETED);
        teachers.saveAndFlush(teacher);
    }

    private School requireSchool(boolean write) {
        Long id = schoolContext.requireSchoolId();
        return (write ? schools.findActiveForUpdate(id) : schools.findByIdAndRowStatus(id, RowStatus.ACTIVE))
                .orElseThrow(() -> ServiceException.notFound("Sekolah tidak ditemukan atau sudah dinonaktifkan"));
    }

    private Teacher find(Long id, Long schoolId) {
        return teachers.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Guru tidak ditemukan di sekolah ini"));
    }

    private Teacher save(Teacher teacher, RequestTeacher request) {
        String code = optional(request.employeeCode());
        if (code != null) code = code.toUpperCase(Locale.ROOT);
        if (code != null && teachers.existsBySchoolIdAndEmployeeCodeIgnoreCaseAndRowStatusAndIdNot(teacher.getSchool().getId(), code,
                RowStatus.ACTIVE, teacher.getId() == null ? 0L : teacher.getId())) {
            throw ServiceException.duplicate("Kode guru sudah dipakai di sekolah ini");
        }
        mapper.update(request, teacher);
        teacher.setEmployeeCode(code);
        teacher.setName(request.name().trim());
        teacher.setEmail(optional(request.email()));
        teacher.setPhone(optional(request.phone()));
        try { return teachers.saveAndFlush(teacher); }
        catch (DataIntegrityViolationException ex) {
            throw ServiceException.duplicate("Kode guru sudah dipakai di sekolah ini");
        }
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
