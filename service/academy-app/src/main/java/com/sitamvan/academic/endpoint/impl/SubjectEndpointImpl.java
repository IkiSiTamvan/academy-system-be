package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestSubject;
import com.sitamvan.academic.dto.response.ResponseSubject;
import com.sitamvan.academic.endpoint.ISubjectEndpoint;
import com.sitamvan.academic.mapper.SubjectMapper;
import com.sitamvan.academic.service.ISubjectService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubjectEndpointImpl implements ISubjectEndpoint {
    private final ISubjectService service;
    private final SubjectMapper mapper;

    @Override
    public BaseResponse<List<ResponseSubject>> search(String q, int page, int size) {
        return ResponseBuilder.ok(service.search(q, page, size).map(mapper::toResponse), "Mata pelajaran dimuat", null);
    }
    @Override
    public BaseResponse<ResponseSubject> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Mata pelajaran dimuat");
    }
    @Override
    public BaseResponse<ResponseSubject> create(RequestSubject request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Mata pelajaran ditambahkan");
    }
    @Override
    public BaseResponse<ResponseSubject> update(Long id, RequestSubject request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Mata pelajaran diperbarui");
    }
    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Mata pelajaran dihapus");
    }
}
