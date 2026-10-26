package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestAcademicYear;
import com.sitamvan.academic.model.AcademicYear;
import org.springframework.data.domain.Page;

public interface IAcademicYearService {
    Page<AcademicYear> search(String query, Boolean active, int page, int size);
    AcademicYear getById(Long id);
    AcademicYear create(RequestAcademicYear request);
    AcademicYear update(Long id, RequestAcademicYear request);
    void delete(Long id);
}
