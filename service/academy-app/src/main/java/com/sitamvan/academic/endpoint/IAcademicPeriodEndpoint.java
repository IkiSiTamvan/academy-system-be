package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestAcademicPeriod;
import com.sitamvan.academic.dto.response.ResponseAcademicPeriod;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/academic-periods")
public interface IAcademicPeriodEndpoint {
    @GetMapping
    BaseResponse<List<ResponseAcademicPeriod>> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}")
    BaseResponse<ResponseAcademicPeriod> getById(@PathVariable Long id);
    @PostMapping
    BaseResponse<ResponseAcademicPeriod> create(@Valid @RequestBody RequestAcademicPeriod request);
    @PutMapping("/{id}")
    BaseResponse<ResponseAcademicPeriod> update(@PathVariable Long id, @Valid @RequestBody RequestAcademicPeriod request);
    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
