package com.sitamvan.auth.security;

import com.sitamvan.auth.model.User;
import com.sitamvan.auth.repository.ISchoolMembershipRepository;
import com.sitamvan.auth.repository.IUserGlobalRoleRepository;
import com.sitamvan.auth.repository.IUserRepository;
import com.sitamvan.shared.data.util.RowStatus;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final IUserRepository userRepository;
    private final IUserGlobalRoleRepository globalRoleRepository;
    private final ISchoolMembershipRepository membershipRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtService.parseUserId(authorization.substring(7));
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.getEnabled() || user.getRowStatus() != RowStatus.ACTIVE) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or inactive account");
                return;
            }

            Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
            globalRoleRepository.findRoleCodesByUserId(userId).stream()
                    .map(SimpleGrantedAuthority::new).forEach(authorities::add);

            String schoolHeader = request.getHeader("X-School-ID");
            if (schoolHeader != null && !schoolHeader.isBlank()) {
                Long schoolId;
                try { schoolId = Long.valueOf(schoolHeader); }
                catch (NumberFormatException ex) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-School-ID must be a numeric school id");
                    return;
                }
                boolean platformAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("PLATFORM_ADMIN"));
                if (!platformAdmin && membershipRepository.findActiveMembership(userId, schoolId).isEmpty()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "No active membership for this school");
                    return;
                }
                membershipRepository.findRoleCodes(userId, schoolId).stream()
                        .map(SimpleGrantedAuthority::new).forEach(authorities::add);
                request.setAttribute("academy.schoolId", schoolId);
            }

            UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired access token");
        }
    }
}
