package com.sitamvan.auth.endpoint;

import com.sitamvan.auth.dto.request.RequestUser;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.dto.statval.ApplicationConstant;
import com.sitamvan.shared.data.base.BaseResponse;
import org.springframework.web.bind.annotation.*;

@RequestMapping(ApplicationConstant.ContextPath.Auth.USER)
public interface IUserEndpoint {

    @PostMapping
    BaseResponse<ResponseUser> createUser(@RequestBody RequestUser request);

    @GetMapping("/{id}")
    BaseResponse<ResponseUser> getUserById(@PathVariable Long id);

    @PutMapping("/{id}")
    BaseResponse<ResponseUser> updateUser(
            @PathVariable Long id,
            @RequestBody RequestUser request);

    @DeleteMapping("/{id}")
    BaseResponse<Long> deleteUser(@PathVariable Long id);
}
