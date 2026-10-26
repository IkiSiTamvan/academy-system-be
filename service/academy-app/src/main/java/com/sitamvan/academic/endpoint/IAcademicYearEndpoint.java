package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestAcademicYear;
import com.sitamvan.academic.dto.response.ResponseAcademicYear;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/academic-years")
public interface IAcademicYearEndpoint {
    @GetMapping
    BaseResponse<List<ResponseAcademicYear>> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);

    @GetMapping("/{id}")
    BaseResponse<ResponseAcademicYear> getById(@PathVariable Long id);

    @PostMapping
    BaseResponse<ResponseAcademicYear> create(@Valid @RequestBody RequestAcademicYear request);

    @PutMapping("/{id}")
    BaseResponse<ResponseAcademicYear> update(@PathVariable Long id, @Valid @RequestBody RequestAcademicYear request);

    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
