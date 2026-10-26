package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestTimeSlot;
import com.sitamvan.academic.dto.response.ResponseTimeSlot;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/time-slots")
public interface ITimeSlotEndpoint {
    @GetMapping
    BaseResponse<List<ResponseTimeSlot>> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId, @RequestParam(required = false) Short dayOfWeek,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}") BaseResponse<ResponseTimeSlot> getById(@PathVariable Long id);
    @PostMapping BaseResponse<ResponseTimeSlot> create(@Valid @RequestBody RequestTimeSlot request);
    @PutMapping("/{id}") BaseResponse<ResponseTimeSlot> update(@PathVariable Long id, @Valid @RequestBody RequestTimeSlot request);
    @DeleteMapping("/{id}") BaseResponse<Void> delete(@PathVariable Long id);
}
