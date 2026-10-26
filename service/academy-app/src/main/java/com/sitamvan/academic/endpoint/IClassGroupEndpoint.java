package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestClassGroup;
import com.sitamvan.academic.dto.response.ResponseClassGroup;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/class-groups")
public interface IClassGroupEndpoint {
    @GetMapping
    BaseResponse<List<ResponseClassGroup>> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long academicYearId, @RequestParam(required = false) Long majorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}")
    BaseResponse<ResponseClassGroup> getById(@PathVariable Long id);
    @PostMapping
    BaseResponse<ResponseClassGroup> create(@Valid @RequestBody RequestClassGroup request);
    @PutMapping("/{id}")
    BaseResponse<ResponseClassGroup> update(@PathVariable Long id, @Valid @RequestBody RequestClassGroup request);
    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
