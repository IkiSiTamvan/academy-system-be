package com.sitamvan.auth.service;

import com.sitamvan.auth.dto.request.RequestUserCreate;
import com.sitamvan.auth.dto.request.RequestUserUpdate;
import com.sitamvan.auth.model.User;

public interface IUserService {
    User createUser(RequestUserCreate request);
    User getUserById(Long id);
    User updateUser(RequestUserUpdate request);
    void deleteUser(Long id);
}
