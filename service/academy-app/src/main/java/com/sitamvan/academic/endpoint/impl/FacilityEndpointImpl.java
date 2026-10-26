package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestFacility;
import com.sitamvan.academic.dto.response.ResponseFacility;
import com.sitamvan.academic.endpoint.IFacilityEndpoint;
import com.sitamvan.academic.mapper.FacilityMapper;
import com.sitamvan.academic.service.IFacilityService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FacilityEndpointImpl implements IFacilityEndpoint {
    private final IFacilityService service;
    private final FacilityMapper mapper;

    @Override
    public BaseResponse<List<ResponseFacility>> search(String q, int page, int size) {
        return ResponseBuilder.ok(service.search(q, page, size).map(mapper::toResponse), "Fasilitas dimuat", null);
    }
    @Override
    public BaseResponse<ResponseFacility> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Fasilitas dimuat");
    }
    @Override
    public BaseResponse<ResponseFacility> create(RequestFacility request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Fasilitas ditambahkan");
    }
    @Override
    public BaseResponse<ResponseFacility> update(Long id, RequestFacility request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Fasilitas diperbarui");
    }
    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Fasilitas dihapus");
    }
}
