package com.sitamvan.auth.endpoint.impl;

import com.sitamvan.auth.dto.request.RequestUserCreate;
import com.sitamvan.auth.dto.request.RequestUserUpdate;
import com.sitamvan.auth.dto.request.UserSearchCriteria;
import com.sitamvan.auth.dto.response.ResponseUser;
import com.sitamvan.auth.endpoint.IUserEndpoint;
import com.sitamvan.auth.mapper.UserMapper;
import com.sitamvan.auth.model.User;
import com.sitamvan.auth.service.IUserService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
public class UserEndpointImpl implements IUserEndpoint {
    private final IUserService userService;
    private final UserMapper userMapper;

    @Override
    public BaseResponse<ResponseUser> createUser(RequestUserCreate request) {
        User user = userService.createUser(request);
        return ResponseBuilder.ok(userMapper.toResponse(user), "User created successfully");
    }

    @Override
    public BaseResponse<ResponseUser> getUserById(Long id) {
        User user = userService.getUserById(id);
        return ResponseBuilder.ok(userMapper.toResponse(user), "User fetched successfully");
    }

    @Override
    public BaseResponse<ResponseUser> updateUser(Long id, RequestUserUpdate request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseBuilder.ok(userMapper.toResponse(updatedUser), "User updated successfully");
    }

    @Override
    public BaseResponse<Long> deleteUser(Long id) {
        userService.deleteUser(id);
        return ResponseBuilder.ok(id, "User deleted successfully");
    }

    @Override
    public BaseResponse<List<ResponseUser>> getUsers(Integer page, Integer size, UserSearchCriteria criteria) {
        Page<User> users = userService.getUsers(criteria, page, size);
        Page<ResponseUser> response = users.map(userMapper::toResponse);
        return ResponseBuilder.ok(response, "Users fetched successfully", null);
    }

}
