package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestAcademicPeriod;
import com.sitamvan.academic.model.AcademicPeriod;
import org.springframework.data.domain.Page;

public interface IAcademicPeriodService {
    Page<AcademicPeriod> search(String q, Long academicYearId, int page, int size);
    AcademicPeriod getById(Long id);
    AcademicPeriod create(RequestAcademicPeriod request);
    AcademicPeriod update(Long id, RequestAcademicPeriod request);
    void delete(Long id);
}
