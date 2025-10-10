package com.sitamvan.auth.endpoint;

import com.sitamvan.auth.dto.request.CreateUserRequest;
import com.sitamvan.auth.dto.request.UpdateUserRequest;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.dto.statval.ApplicationConstant;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RequestMapping(ApplicationConstant.ContextPath.Auth.USER)
public interface IUserEndpoint {

    @PostMapping
    BaseResponse<ResponseUser> createUser(@Valid @RequestBody CreateUserRequest request);

    @GetMapping("/{id}")
    BaseResponse<ResponseUser> getUserById(@PathVariable Long id);

    @PutMapping()
    BaseResponse<ResponseUser> updateUser(
            @Valid @RequestBody UpdateUserRequest request);

    @DeleteMapping("/{id}")
    BaseResponse<Long> deleteUser(@PathVariable Long id);
}
