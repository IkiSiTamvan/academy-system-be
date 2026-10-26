package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestMajor;
import com.sitamvan.academic.dto.response.ResponseMajor;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/majors")
public interface IMajorEndpoint {
    @GetMapping
    BaseResponse<List<ResponseMajor>> search(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}")
    BaseResponse<ResponseMajor> getById(@PathVariable Long id);
    @PostMapping
    BaseResponse<ResponseMajor> create(@Valid @RequestBody RequestMajor request);
    @PutMapping("/{id}")
    BaseResponse<ResponseMajor> update(@PathVariable Long id, @Valid @RequestBody RequestMajor request);
    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
