package com.sitamvan.auth.endpoint;

import com.sitamvan.auth.dto.request.RequestUserCreate;
import com.sitamvan.auth.dto.request.RequestUserUpdate;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.dto.statval.ApplicationConstant;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequestMapping(ApplicationConstant.ContextPath.Auth.USER)
public interface IUserEndpoint {

    @PostMapping
    BaseResponse<ResponseUser> createUser(@Valid @RequestBody RequestUserCreate request);

    @GetMapping("/{id}")
    BaseResponse<ResponseUser> getUserById(@PathVariable Long id);

    @PutMapping()
    BaseResponse<ResponseUser> updateUser(
            @Valid @RequestBody RequestUserUpdate request);

    @DeleteMapping("/{id}")
    BaseResponse<Long> deleteUser(@PathVariable Long id);
}
