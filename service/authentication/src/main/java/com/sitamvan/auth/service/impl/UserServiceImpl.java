package com.sitamvan.auth.service.impl;

import com.sitamvan.auth.dto.request.CreateUserRequest;
import com.sitamvan.auth.dto.request.UpdateUserRequest;
import com.sitamvan.auth.mapper.UserMapper;
import com.sitamvan.auth.model.User;
import com.sitamvan.auth.repository.IUserRepository;
import com.sitamvan.auth.service.IUserService;
import com.sitamvan.shared.data.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements IUserService {
    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public User createUser(CreateUserRequest request) {
        try {

            User user = userMapper.toEntityFromCreate(request);

            Optional.ofNullable(request.password())
                    .filter(p -> !p.isBlank())
                    .map(passwordEncoder::encode)
                    .ifPresent(user::setPassword);

            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw mapConstraintToField(e);
        }
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ServiceException.notFound(USER_NOT_FOUND_MSG + id));
    }

    @Override
    public User updateUser(UpdateUserRequest request) {
        try {
            User existingUser = userRepository.findById(request.id())
                    .orElseThrow(() -> ServiceException.notFound(USER_NOT_FOUND_MSG + request.id()));

            userMapper.toEntityFromUpdate(request, existingUser);

            Optional.ofNullable(request.password())
                    .filter(p -> !p.isBlank())
                    .map(passwordEncoder::encode)
                    .ifPresent(existingUser::setPassword);

            return userRepository.save(existingUser);
        } catch (DataIntegrityViolationException e) {
            throw mapConstraintToField(e);
        }
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> ServiceException.notFound(USER_NOT_FOUND_MSG + id));
        userRepository.delete(user);
    }

    private ServiceException mapConstraintToField(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message.contains("uk_user_username")) {
            return ServiceException.badRequest("Username already exists");
        } else if (message.contains("uk_user_email")) {
            return ServiceException.badRequest("Email already exists");
        }

        return ServiceException.badRequest("Data integrity error: " + message);
    }
}
