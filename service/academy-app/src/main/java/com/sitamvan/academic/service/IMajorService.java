package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestMajor;
import com.sitamvan.academic.model.Major;
import org.springframework.data.domain.Page;

public interface IMajorService {
    Page<Major> search(String query, int page, int size);
    Major getById(Long id);
    Major create(RequestMajor request);
    Major update(Long id, RequestMajor request);
    void delete(Long id);
}
