package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestMajor;
import com.sitamvan.academic.dto.response.ResponseMajor;
import com.sitamvan.academic.endpoint.IMajorEndpoint;
import com.sitamvan.academic.mapper.MajorMapper;
import com.sitamvan.academic.service.IMajorService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MajorEndpointImpl implements IMajorEndpoint {
    private final IMajorService service;
    private final MajorMapper mapper;

    @Override
    public BaseResponse<List<ResponseMajor>> search(String q, int page, int size) {
        return ResponseBuilder.ok(service.search(q, page, size).map(mapper::toResponse), "Jurusan dimuat", null);
    }
    @Override
    public BaseResponse<ResponseMajor> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Jurusan dimuat");
    }
    @Override
    public BaseResponse<ResponseMajor> create(RequestMajor request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Jurusan ditambahkan");
    }
    @Override
    public BaseResponse<ResponseMajor> update(Long id, RequestMajor request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Jurusan diperbarui");
    }
    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Jurusan dihapus");
    }
}
