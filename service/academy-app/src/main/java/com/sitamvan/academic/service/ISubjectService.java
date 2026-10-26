package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestSubject;
import com.sitamvan.academic.model.Subject;
import org.springframework.data.domain.Page;

public interface ISubjectService {
    Page<Subject> search(String query, int page, int size);
    Subject getById(Long id);
    Subject create(RequestSubject request);
    Subject update(Long id, RequestSubject request);
    void delete(Long id);
}
