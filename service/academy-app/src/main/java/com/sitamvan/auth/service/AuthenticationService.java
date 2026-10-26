package com.sitamvan.auth.service;

import com.sitamvan.auth.dto.response.ResponseAuth;
import com.sitamvan.auth.dto.response.ResponseCurrentUser;
import com.sitamvan.auth.dto.response.ResponseMembership;
import com.sitamvan.auth.model.User;
import com.sitamvan.auth.repository.ISchoolMembershipRepository;
import com.sitamvan.auth.repository.IUserGlobalRoleRepository;
import com.sitamvan.auth.repository.IUserRepository;
import com.sitamvan.auth.security.JwtService;
import com.sitamvan.shared.data.exception.ServiceException;
import com.sitamvan.shared.data.util.RowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {
    private final IUserRepository userRepository;
    private final IUserGlobalRoleRepository globalRoleRepository;
    private final ISchoolMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ResponseAuth login(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .filter(User::getEnabled)
                .filter(candidate -> candidate.getRowStatus() == RowStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPassword()))
                .orElseThrow(() -> ServiceException.unauthorized("Invalid username or password"));
        return new ResponseAuth(jwtService.issue(user.getId()), "Bearer", jwtService.expiresInSeconds());
    }

    public ResponseCurrentUser currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .filter(candidate -> candidate.getRowStatus() == RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("User not found"));
        var memberships = membershipRepository.findActiveMembershipsByUserId(userId).stream()
                .map(membership -> new ResponseMembership(
                        membership.getSchool().getId(), membership.getSchool().getCode(), membership.getSchool().getName(),
                        membershipRepository.findRoleCodes(userId, membership.getSchool().getId())))
                .toList();
        return new ResponseCurrentUser(user.getId(), user.getUsername(), user.getEmail(),
                globalRoleRepository.findRoleCodesByUserId(userId), memberships);
    }
}
