package com.sitamvan.auth.service;

import com.sitamvan.auth.dto.request.RequestUser;
import com.sitamvan.auth.model.User;

public interface IUserService {
    User createUser(RequestUser request);
    User getUserById(Long id);
    User updateUser(Long id, RequestUser request);
    void deleteUser(Long id);
}
