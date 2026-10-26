package com.sitamvan.academic.endpoint;

import com.sitamvan.academic.dto.request.RequestRoom;
import com.sitamvan.academic.dto.response.ResponseRoom;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/v1/rooms")
public interface IRoomEndpoint {
    @GetMapping
    BaseResponse<List<ResponseRoom>> search(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);
    @GetMapping("/{id}") BaseResponse<ResponseRoom> getById(@PathVariable Long id);
    @PostMapping BaseResponse<ResponseRoom> create(@Valid @RequestBody RequestRoom request);
    @PutMapping("/{id}") BaseResponse<ResponseRoom> update(@PathVariable Long id, @Valid @RequestBody RequestRoom request);
    @DeleteMapping("/{id}") BaseResponse<Void> delete(@PathVariable Long id);
}
