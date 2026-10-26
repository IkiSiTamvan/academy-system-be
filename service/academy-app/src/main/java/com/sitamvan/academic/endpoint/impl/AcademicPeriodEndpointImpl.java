package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestAcademicPeriod;
import com.sitamvan.academic.dto.response.ResponseAcademicPeriod;
import com.sitamvan.academic.endpoint.IAcademicPeriodEndpoint;
import com.sitamvan.academic.mapper.AcademicPeriodMapper;
import com.sitamvan.academic.service.IAcademicPeriodService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AcademicPeriodEndpointImpl implements IAcademicPeriodEndpoint {
    private final IAcademicPeriodService service;
    private final AcademicPeriodMapper mapper;

    @Override
    public BaseResponse<List<ResponseAcademicPeriod>> search(String q, Long academicYearId, int page, int size) {
        return ResponseBuilder.ok(service.search(q, academicYearId, page, size).map(mapper::toResponse), "Periode dimuat", null);
    }
    @Override
    public BaseResponse<ResponseAcademicPeriod> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Periode dimuat");
    }
    @Override
    public BaseResponse<ResponseAcademicPeriod> create(RequestAcademicPeriod request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Periode ditambahkan");
    }
    @Override
    public BaseResponse<ResponseAcademicPeriod> update(Long id, RequestAcademicPeriod request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Periode diperbarui");
    }
    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Periode dihapus");
    }
}
