package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestRoom;
import com.sitamvan.academic.dto.response.ResponseRoom;
import com.sitamvan.academic.endpoint.IRoomEndpoint;
import com.sitamvan.academic.service.IRoomService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomEndpointImpl implements IRoomEndpoint {
    private final IRoomService service;
    @Override public BaseResponse<List<ResponseRoom>> search(String q, int page, int size) { return ResponseBuilder.ok(service.search(q, page, size), "Ruangan dimuat", null); }
    @Override public BaseResponse<ResponseRoom> getById(Long id) { return ResponseBuilder.ok(service.getById(id), "Ruangan dimuat"); }
    @Override public BaseResponse<ResponseRoom> create(RequestRoom request) { return ResponseBuilder.ok(service.create(request), "Ruangan ditambahkan"); }
    @Override public BaseResponse<ResponseRoom> update(Long id, RequestRoom request) { return ResponseBuilder.ok(service.update(id, request), "Ruangan diperbarui"); }
    @Override public BaseResponse<Void> delete(Long id) { service.delete(id); return ResponseBuilder.ok((Void) null, "Ruangan dihapus"); }
}
