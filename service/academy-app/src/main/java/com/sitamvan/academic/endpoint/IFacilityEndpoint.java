package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestFacility;
import com.sitamvan.academic.dto.response.ResponseFacility;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/facilities")
public interface IFacilityEndpoint {
    @GetMapping
    BaseResponse<List<ResponseFacility>> search(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}")
    BaseResponse<ResponseFacility> getById(@PathVariable Long id);
    @PostMapping
    BaseResponse<ResponseFacility> create(@Valid @RequestBody RequestFacility request);
    @PutMapping("/{id}")
    BaseResponse<ResponseFacility> update(@PathVariable Long id, @Valid @RequestBody RequestFacility request);
    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
