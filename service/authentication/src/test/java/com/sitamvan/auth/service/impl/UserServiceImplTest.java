package com.sitamvan.auth.service.impl;

import com.sitamvan.auth.dto.request.CreateUserRequest;
import com.sitamvan.auth.dto.request.UpdateUserRequest;
import com.sitamvan.auth.mapper.UserMapper;
import com.sitamvan.auth.model.User;
import com.sitamvan.auth.repository.IUserRepository;
import com.sitamvan.shared.data.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private IUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private CreateUserRequest buildCreateRequest(String username, String email, String password, Boolean isActive) {
        return CreateUserRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .isActive(isActive)
                .build();
    }

    private UpdateUserRequest buildUpdateRequest(Long id, String username, String email, String password, Boolean isActive) {
        return UpdateUserRequest.builder()
                .id(id)
                .username(username)
                .email(email)
                .password(password)
                .isActive(isActive)
                .build();
    }

    @Test
    void createUser_shouldEncodePassword_andSave() {
        // arrange
        CreateUserRequest req = buildCreateRequest("john", "john@mail.com", "secret", true);
        User mapped = new User();
        when(userMapper.toEntityFromCreate(req)).thenReturn(mapped);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(mapped)).thenAnswer(inv -> inv.getArgument(0));

        // act
        User result = userService.createUser(req);

        // assert
        assertThat(result).isSameAs(mapped);
        assertThat(mapped.getPassword()).isEqualTo("encoded");
        verify(userRepository).save(mapped);
    }

    @Test
    void createUser_shouldNotEncode_whenPasswordBlank() {
        // arrange
        CreateUserRequest req = buildCreateRequest("john", "john@mail.com", "  ", true);
        User mapped = new User();
        when(userMapper.toEntityFromCreate(req)).thenReturn(mapped);
        when(userRepository.save(mapped)).thenAnswer(inv -> inv.getArgument(0));

        // act
        User result = userService.createUser(req);

        // assert
        assertThat(result).isSameAs(mapped);
        assertThat(mapped.getPassword()).isNull();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createUser_shouldMapConstraintViolation_toBadRequest() {
        // arrange
        CreateUserRequest req = buildCreateRequest("john", "john@mail.com", "secret", true);
        User mapped = new User();
        when(userMapper.toEntityFromCreate(req)).thenReturn(mapped);
        DataIntegrityViolationException dive = new DataIntegrityViolationException("duplicate key value violates unique constraint uk_user_email");
        when(userRepository.save(any(User.class))).thenThrow(dive);

        // act + assert
        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getUserById_shouldReturn_whenFound() {
        // arrange
        User user = new User();
        user.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        // act
        User result = userService.getUserById(10L);

        // assert
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getUserById_shouldThrow_whenNotFound() {
        // arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // act + assert
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    void updateUser_shouldApplyMapper_andEncodePassword_whenProvided() {
        // arrange
        UpdateUserRequest req = buildUpdateRequest(5L, "newname", "new@mail.com", "newpass", true);
        User existing = new User();
        existing.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass")).thenReturn("enc");
        when(userRepository.save(existing)).thenReturn(existing);

        // act
        User updated = userService.updateUser(req);

        // assert
        assertThat(updated).isSameAs(existing);
        assertThat(existing.getPassword()).isEqualTo("enc");
        verify(userMapper).toEntityFromUpdate(req, existing);
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_shouldNotEncode_whenPasswordNullOrBlank() {
        // arrange
        UpdateUserRequest req = buildUpdateRequest(7L, null, null, null, null);
        User existing = new User();
        existing.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        // act
        userService.updateUser(req);

        // assert
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_shouldThrow_whenNotFound() {
        // arrange
        UpdateUserRequest req = buildUpdateRequest(404L, null, null, null, null);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        // act + assert
        assertThatThrownBy(() -> userService.updateUser(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("User not found with id: 404");
    }

    @Test
    void deleteUser_shouldDelete_whenFound() {
        // arrange
        User existing = new User();
        existing.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));

        // act
        userService.deleteUser(3L);

        // assert
        verify(userRepository).delete(existing);
    }

    @Test
    void deleteUser_shouldThrow_whenNotFound() {
        // arrange
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // act + assert
        assertThatThrownBy(() -> userService.deleteUser(2L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("User not found with id: 2");
    }
}
