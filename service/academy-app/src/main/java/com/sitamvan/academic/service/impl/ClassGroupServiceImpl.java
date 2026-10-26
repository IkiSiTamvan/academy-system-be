package com.sitamvan.academic.service.impl;

import com.sitamvan.academic.dto.request.RequestClassGroup;
import com.sitamvan.academic.mapper.ClassGroupMapper;
import com.sitamvan.academic.model.*;
import com.sitamvan.academic.repository.*;
import com.sitamvan.academic.service.IClassGroupService;
import com.sitamvan.academic.specifications.ClassGroupSpecifications;
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
public class ClassGroupServiceImpl implements IClassGroupService {
    private final IClassGroupRepository classes;
    private final IAcademicYearRepository years;
    private final IMajorRepository majors;
    private final ISchoolRepository schools;
    private final SchoolContext schoolContext;
    private final ClassGroupMapper mapper;

    @Override
    public Page<ClassGroup> search(String q, Long academicYearId, Long majorId, int page, int size) {
        Long schoolId = requireSchool(false).getId();
        if (page < 0 || size < 1 || size > 100 || (q != null && q.length() > 100)) {
            throw ServiceException.badRequest("Parameter pencarian tidak valid; ukuran halaman harus 1–100");
        }
        if (academicYearId != null) requireYear(academicYearId, schoolId);
        if (majorId != null) requireMajor(majorId, schoolId);
        return classes.findAll(Specification.allOf(ClassGroupSpecifications.forSchool(schoolId),
                ClassGroupSpecifications.forYear(academicYearId), ClassGroupSpecifications.forMajor(majorId),
                ClassGroupSpecifications.search(q)), PageRequest.of(page, size, Sort.by("gradeLevel", "name", "id")));
    }
    @Override public ClassGroup getById(Long id) { return findClass(id, requireSchool(false).getId()); }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public ClassGroup create(RequestClassGroup request) {
        School school = requireSchool(true);
        ClassGroup item = new ClassGroup();
        item.setSchool(school);
        return save(item, request, school.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public ClassGroup update(Long id, RequestClassGroup request) {
        Long schoolId = requireSchool(true).getId();
        ClassGroup item = findClass(id, schoolId);
        if ((!item.getAcademicYear().getId().equals(request.academicYearId()) || !item.getMajor().getId().equals(request.majorId()))
                && classes.hasDependencies(id)) {
            throw ServiceException.badRequest("Tahun ajaran dan jurusan tidak dapat diganti karena kelas sudah digunakan");
        }
        return save(item, request, schoolId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'SCHEDULER')")
    public void delete(Long id) {
        ClassGroup item = findClass(id, requireSchool(true).getId());
        if (classes.hasDependencies(id)) throw ServiceException.badRequest("Kelas masih digunakan oleh kebutuhan pelajaran atau jadwal");
        item.setRowStatus(RowStatus.DELETED);
        classes.saveAndFlush(item);
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
    private Major requireMajor(Long id, Long schoolId) {
        return majors.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Jurusan tidak ditemukan di sekolah ini"));
    }
    private ClassGroup findClass(Long id, Long schoolId) {
        ClassGroup item = classes.findByIdAndSchoolIdAndRowStatus(id, schoolId, RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("Kelas tidak ditemukan di sekolah ini"));
        if (item.getAcademicYear().getRowStatus() != RowStatus.ACTIVE || item.getMajor().getRowStatus() != RowStatus.ACTIVE) {
            throw ServiceException.notFound("Tahun ajaran atau jurusan kelas sudah dihapus");
        }
        return item;
    }
    private ClassGroup save(ClassGroup item, RequestClassGroup request, Long schoolId) {
        AcademicYear year = requireYear(request.academicYearId(), schoolId);
        Major major = requireMajor(request.majorId(), schoolId);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (classes.existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCaseAndRowStatusAndIdNot(schoolId, year.getId(), code,
                RowStatus.ACTIVE, item.getId() == null ? 0L : item.getId())) {
            throw ServiceException.duplicate("Kode kelas sudah dipakai pada tahun ajaran ini");
        }
        mapper.update(request, item);
        item.setAcademicYear(year);
        item.setMajor(major);
        item.setCode(code);
        item.setName(request.name().trim());
        item.setSection(request.section().trim());
        try { return classes.saveAndFlush(item); }
        catch (DataIntegrityViolationException ex) { throw ServiceException.duplicate("Kode kelas sudah dipakai pada tahun ajaran ini"); }
    }
}
