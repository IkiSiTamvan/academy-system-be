package com.sitamvan.auth.service;

import com.sitamvan.auth.model.User;
import com.sitamvan.auth.repository.ISchoolMembershipRepository;
import com.sitamvan.auth.repository.IUserGlobalRoleRepository;
import com.sitamvan.auth.repository.IUserRepository;
import com.sitamvan.auth.security.JwtService;
import com.sitamvan.shared.data.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock IUserRepository userRepository;
    @Mock IUserGlobalRoleRepository globalRoleRepository;
    @Mock ISchoolMembershipRepository membershipRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthenticationService authenticationService;

    @Test
    void loginIssuesConfiguredStatelessAccessToken() {
        User user = new User();
        user.setId(7L);
        user.setUsername("school-admin");
        user.setPassword("encoded-password");
        when(userRepository.findByUsernameIgnoreCase("school-admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtService.issue(7L)).thenReturn("signed-token");
        when(jwtService.expiresInSeconds()).thenReturn(86_400L);

        var response = authenticationService.login("school-admin", "password");

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(86_400L);
        verify(jwtService).issue(7L);
    }

    @Test
    void loginUsesSameUnauthorizedErrorForUnknownUserAndWrongPassword() {
        when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login("missing", "wrong"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Invalid username or password");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
