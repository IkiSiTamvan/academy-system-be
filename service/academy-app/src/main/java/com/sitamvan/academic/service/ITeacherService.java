package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestTeacher;
import com.sitamvan.academic.model.Teacher;
import org.springframework.data.domain.Page;

public interface ITeacherService {
    Page<Teacher> search(String query, Boolean active, int page, int size);
    Teacher getById(Long id);
    Teacher create(RequestTeacher request);
    Teacher update(Long id, RequestTeacher request);
    void delete(Long id);
}
