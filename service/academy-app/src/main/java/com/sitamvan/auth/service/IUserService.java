package com.sitamvan.auth.service;

import com.sitamvan.auth.dto.request.RequestUserCreate;
import com.sitamvan.auth.dto.request.RequestUserUpdate;
import com.sitamvan.auth.dto.request.UserSearchCriteria;
import com.sitamvan.auth.model.User;
import org.springframework.data.domain.Page;

public interface IUserService {
    User createUser(RequestUserCreate request);
    User getUserById(Long id);
    User updateUser(Long id, RequestUserUpdate request);
    void deleteUser(Long id);
    Page<User> getUsers(UserSearchCriteria criteria, Integer page, Integer size);

}
