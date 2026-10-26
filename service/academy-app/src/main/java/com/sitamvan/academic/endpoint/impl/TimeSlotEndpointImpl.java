package com.sitamvan.academic.endpoint.impl;

import com.sitamvan.academic.dto.request.RequestTimeSlot;
import com.sitamvan.academic.dto.response.ResponseTimeSlot;
import com.sitamvan.academic.endpoint.ITimeSlotEndpoint;
import com.sitamvan.academic.mapper.TimeSlotMapper;
import com.sitamvan.academic.service.ITimeSlotService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimeSlotEndpointImpl implements ITimeSlotEndpoint {
    private final ITimeSlotService service;
    private final TimeSlotMapper mapper;
    @Override public BaseResponse<List<ResponseTimeSlot>> search(String q, Long academicYearId, Short dayOfWeek, int page, int size) {
        return ResponseBuilder.ok(service.search(q, academicYearId, dayOfWeek, page, size).map(mapper::toResponse), "Slot waktu dimuat", null);
    }
    @Override public BaseResponse<ResponseTimeSlot> getById(Long id) { return ResponseBuilder.ok(mapper.toResponse(service.getById(id)), "Slot waktu dimuat"); }
    @Override public BaseResponse<ResponseTimeSlot> create(RequestTimeSlot request) { return ResponseBuilder.ok(mapper.toResponse(service.create(request)), "Slot waktu ditambahkan"); }
    @Override public BaseResponse<ResponseTimeSlot> update(Long id, RequestTimeSlot request) { return ResponseBuilder.ok(mapper.toResponse(service.update(id, request)), "Slot waktu diperbarui"); }
    @Override public BaseResponse<Void> delete(Long id) { service.delete(id); return ResponseBuilder.ok((Void) null, "Slot waktu dihapus"); }
}
