package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestTeacher;
import com.sitamvan.academic.dto.response.ResponseTeacher;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/teachers")
public interface ITeacherEndpoint {
    @GetMapping
    BaseResponse<List<ResponseTeacher>> search(@RequestParam(required = false) String q, @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}")
    BaseResponse<ResponseTeacher> getById(@PathVariable Long id);
    @PostMapping
    BaseResponse<ResponseTeacher> create(@Valid @RequestBody RequestTeacher request);
    @PutMapping("/{id}")
    BaseResponse<ResponseTeacher> update(@PathVariable Long id, @Valid @RequestBody RequestTeacher request);
    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
