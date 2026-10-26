package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestClassGroup;
import com.sitamvan.academic.dto.response.ResponseClassGroup;
import com.sitamvan.academic.endpoint.IClassGroupEndpoint;
import com.sitamvan.academic.mapper.ClassGroupMapper;
import com.sitamvan.academic.service.IClassGroupService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassGroupEndpointImpl implements IClassGroupEndpoint {
    private final IClassGroupService service;
    private final ClassGroupMapper mapper;
    @Override
    public BaseResponse<List<ResponseClassGroup>> search(String q, Long academicYearId, Long majorId, int page, int size) {
        return ResponseBuilder.ok(service.search(q, academicYearId, majorId, page, size).map(mapper::toResponse), "Kelas dimuat", null);
    }
    @Override public BaseResponse<ResponseClassGroup> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Kelas dimuat");
    }
    @Override public BaseResponse<ResponseClassGroup> create(RequestClassGroup request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Kelas ditambahkan");
    }
    @Override public BaseResponse<ResponseClassGroup> update(Long id, RequestClassGroup request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Kelas diperbarui");
    }
    @Override public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Kelas dihapus");
    }
}
