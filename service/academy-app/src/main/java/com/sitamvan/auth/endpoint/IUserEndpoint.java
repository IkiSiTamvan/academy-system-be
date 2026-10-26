package com.sitamvan.auth.endpoint;

import com.sitamvan.auth.dto.request.RequestUserCreate;
import com.sitamvan.auth.dto.request.RequestUserUpdate;
import com.sitamvan.auth.dto.request.UserSearchCriteria;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.dto.statval.ApplicationConstant;
import com.sitamvan.shared.data.base.BaseResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(ApplicationConstant.ContextPath.Auth.USER)
public interface IUserEndpoint {

    @PostMapping
    BaseResponse<ResponseUser> createUser(@Valid @RequestBody RequestUserCreate request);

    @GetMapping(ApplicationConstant.Path.FIND_BY_ID)
    BaseResponse<ResponseUser> getUserById(@PathVariable Long id);

    @PutMapping(ApplicationConstant.Path.FIND_BY_ID)
    BaseResponse<ResponseUser> updateUser(
            @PathVariable(required = true) Long id,
            @Valid @RequestBody RequestUserUpdate request);

    @DeleteMapping(ApplicationConstant.Path.FIND_BY_ID)
    BaseResponse<Long> deleteUser(@PathVariable Long id);

    @GetMapping()
    BaseResponse<List<ResponseUser>> getUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @ParameterObject @ModelAttribute UserSearchCriteria criteria);
}
