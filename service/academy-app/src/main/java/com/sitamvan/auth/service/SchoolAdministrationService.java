package com.sitamvan.auth.service;

import com.sitamvan.academic.model.School;
import com.sitamvan.academic.repository.ISchoolRepository;
import com.sitamvan.auth.dto.request.RequestAcceptInvitation;
import com.sitamvan.auth.dto.request.RequestCreateInvitation;
import com.sitamvan.auth.dto.request.RequestCreateSchool;
import com.sitamvan.auth.dto.response.ResponseInvitation;
import com.sitamvan.auth.dto.response.ResponseInvitationAccepted;
import com.sitamvan.auth.model.*;
import com.sitamvan.auth.repository.*;
import com.sitamvan.shared.data.exception.ServiceException;
import com.sitamvan.shared.data.util.RowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class SchoolAdministrationService {
    private static final Set<String> SCHOOL_ROLE_CODES = Set.of("SCHOOL_ADMIN", "SCHEDULER", "TEACHER", "VIEWER");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ISchoolRepository schoolRepository;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final ISchoolMembershipRepository membershipRepository;
    private final ISchoolMembershipRoleRepository membershipRoleRepository;
    private final ISchoolInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final IUserGlobalRoleRepository globalRoleRepository;

    @Transactional(readOnly = true)
    public List<School> listSchools(Long userId) {
        if (globalRoleRepository.findRoleCodesByUserId(userId).contains("PLATFORM_ADMIN")) {
            return schoolRepository.findByRowStatusOrderByNameAsc(RowStatus.ACTIVE);
        }
        return membershipRepository.findActiveMembershipsByUserId(userId).stream()
                .map(SchoolMembership::getSchool)
                .sorted(Comparator.comparing(School::getName))
                .toList();
    }

    @Transactional
    public School createSchool(RequestCreateSchool request) {
        School school = new School();
        school.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        school.setName(request.name().trim());
        school.setAddress(request.address());
        school.setTimeZone(request.timeZone() == null || request.timeZone().isBlank() ? "Asia/Jakarta" : request.timeZone());
        try { return schoolRepository.saveAndFlush(school); }
        catch (DataIntegrityViolationException ex) { throw ServiceException.duplicate("School code already exists"); }
    }

    @Transactional
    public ResponseInvitation createInvitation(Long schoolId, RequestCreateInvitation request) {
        String roleCode = request.roleCode().trim().toUpperCase(Locale.ROOT);
        if (!SCHOOL_ROLE_CODES.contains(roleCode)) throw ServiceException.badRequest("Unsupported school role: " + roleCode);
        School school = schoolRepository.findById(schoolId)
                .filter(value -> value.getRowStatus() == RowStatus.ACTIVE)
                .orElseThrow(() -> ServiceException.notFound("School not found"));
        Role role = roleRepository.findByCode(roleCode).orElseThrow(() -> ServiceException.internal("Role catalog is incomplete"));

        byte[] randomToken = new byte[32];
        RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        SchoolInvitation invitation = new SchoolInvitation();
        invitation.setSchool(school);
        invitation.setRole(role);
        invitation.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        invitation.setTokenHash(sha256(rawToken));
        invitation.setExpiresAt(expiresAt);
        invitationRepository.save(invitation);
        return new ResponseInvitation(invitation.getEmail(), school.getName(), roleCode, rawToken, expiresAt);
    }

    @Transactional
    public ResponseInvitationAccepted acceptInvitation(RequestAcceptInvitation request) {
        SchoolInvitation invitation = invitationRepository.findActiveForUpdate(sha256(request.token()))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ServiceException.badRequest("Invitation is invalid, expired, or already accepted"));

        User user = userRepository.findByEmailIgnoreCase(invitation.getEmail()).orElse(null);
        if (user == null) {
            if (userRepository.findByUsernameIgnoreCase(request.username()).isPresent()) {
                throw ServiceException.duplicate("Username already exists");
            }
            user = new User();
            user.setUsername(request.username().trim());
            user.setEmail(invitation.getEmail());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setEnabled(true);
            user = userRepository.save(user);
        } else if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ServiceException.unauthorized("Invalid account password");
        }
        if (!user.getEnabled() || user.getRowStatus() != RowStatus.ACTIVE) {
            throw ServiceException.forbidden("This account is inactive");
        }

        SchoolMembership membership = membershipRepository.findByUserIdAndSchoolId(user.getId(), invitation.getSchool().getId())
                .orElseGet(SchoolMembership::new);
        membership.setUser(user);
        membership.setSchool(invitation.getSchool());
        membership.setEnabled(true);
        membership.setRowStatus(RowStatus.ACTIVE);
        membership = membershipRepository.save(membership);

        String roleCode = invitation.getRole().getCode();
        if (!membershipRoleRepository.existsByMembership_IdAndRole_Code(membership.getId(), roleCode)) {
            SchoolMembershipRole assignment = new SchoolMembershipRole();
            assignment.setMembership(membership);
            assignment.setRole(invitation.getRole());
            membershipRoleRepository.save(assignment);
        }
        invitation.setAcceptedAt(Instant.now());
        return new ResponseInvitationAccepted(user.getId(), invitation.getSchool().getId(), invitation.getSchool().getName(), roleCode);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }
}
