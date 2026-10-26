package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestClassGroup;
import com.sitamvan.academic.model.ClassGroup;
import org.springframework.data.domain.Page;

public interface IClassGroupService {
    Page<ClassGroup> search(String q, Long academicYearId, Long majorId, int page, int size);
    ClassGroup getById(Long id);
    ClassGroup create(RequestClassGroup request);
    ClassGroup update(Long id, RequestClassGroup request);
    void delete(Long id);
}
