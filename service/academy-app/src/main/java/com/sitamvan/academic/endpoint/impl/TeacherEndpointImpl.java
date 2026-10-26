package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestTeacher;
import com.sitamvan.academic.dto.response.ResponseTeacher;
import com.sitamvan.academic.endpoint.ITeacherEndpoint;
import com.sitamvan.academic.mapper.TeacherMapper;
import com.sitamvan.academic.service.ITeacherService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeacherEndpointImpl implements ITeacherEndpoint {
    private final ITeacherService service;
    private final TeacherMapper mapper;

    @Override
    public BaseResponse<List<ResponseTeacher>> search(String q, Boolean active, int page, int size) {
        return ResponseBuilder.ok(service.search(q, active, page, size).map(mapper::toResponse), "Guru dimuat", null);
    }
    @Override
    public BaseResponse<ResponseTeacher> getById(Long id) {
        return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Guru dimuat");
    }
    @Override
    public BaseResponse<ResponseTeacher> create(RequestTeacher request) {
        return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Guru ditambahkan");
    }
    @Override
    public BaseResponse<ResponseTeacher> update(Long id, RequestTeacher request) {
        return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Guru diperbarui");
    }
    @Override
    public BaseResponse<Void> delete(Long id) {
        service.delete(id);
        return ResponseBuilder.ok((Void) null, "Guru dihapus");
    }
}
