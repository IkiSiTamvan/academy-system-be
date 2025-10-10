package com.sitamvan.auth.service;

import com.sitamvan.auth.dto.request.CreateUserRequest;
import com.sitamvan.auth.dto.request.UpdateUserRequest;
import com.sitamvan.auth.model.User;

public interface IUserService {
    User createUser(CreateUserRequest request);
    User getUserById(Long id);
    User updateUser(UpdateUserRequest request);
    void deleteUser(Long id);
}
