package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestAcademicYear;
import com.sitamvan.academic.dto.response.ResponseAcademicYear;
import com.sitamvan.academic.endpoint.IAcademicYearEndpoint;
import com.sitamvan.academic.mapper.AcademicYearMapper;
import com.sitamvan.academic.service.IAcademicYearService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AcademicYearEndpointImpl implements IAcademicYearEndpoint {
    private final IAcademicYearService service;
    private final AcademicYearMapper mapper;

    @Override
    public BaseResponse<List<ResponseAcademicYear>> search(String q, Boolean active, int page, int size) {
        return ResponseBuilder.ok(service.search(q, active, page, size).map(mapper::toResponse), "Tahun ajaran dimuat", null);
    }

    @Override
    public BaseResponse<ResponseAcademicYear> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Tahun ajaran dimuat");
    }

    @Override
    public BaseResponse<ResponseAcademicYear> create(RequestAcademicYear request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Tahun ajaran ditambahkan");
    }

    @Override
    public BaseResponse<ResponseAcademicYear> update(Long id, RequestAcademicYear request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Tahun ajaran diperbarui");
    }

    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Tahun ajaran dihapus");
    }
}
