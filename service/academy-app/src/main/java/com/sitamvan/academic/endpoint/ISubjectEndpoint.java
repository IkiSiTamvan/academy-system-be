package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestSubject;
import com.sitamvan.academic.dto.response.ResponseSubject;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/subjects")
public interface ISubjectEndpoint {
    @GetMapping
    BaseResponse<List<ResponseSubject>> search(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}")
    BaseResponse<ResponseSubject> getById(@PathVariable Long id);
    @PostMapping
    BaseResponse<ResponseSubject> create(@Valid @RequestBody RequestSubject request);
    @PutMapping("/{id}")
    BaseResponse<ResponseSubject> update(@PathVariable Long id, @Valid @RequestBody RequestSubject request);
    @DeleteMapping("/{id}")
    BaseResponse<Void> delete(@PathVariable Long id);
}
