package com.sitamvan.academic.service;

import com.sitamvan.academic.dto.request.RequestRoom;
import com.sitamvan.academic.dto.response.ResponseRoom;
import org.springframework.data.domain.Page;

public interface IRoomService {
    Page<ResponseRoom> search(String query, int page, int size);
    ResponseRoom getById(Long id);
    ResponseRoom create(RequestRoom request);
    ResponseRoom update(Long id, RequestRoom request);
    void delete(Long id);
}
