package com.sitamvan.auth.security;

import com.sitamvan.shared.data.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reads only the school context already validated by JwtAuthenticationFilter. */
@Component
@RequiredArgsConstructor
public class SchoolContext {
    private final HttpServletRequest request;

    public Long requireSchoolId() {
        if (request.getAttribute("academy.schoolId") instanceof Long schoolId && schoolId > 0) {
            return schoolId;
        }
        throw ServiceException.badRequest("Pilih sekolah terlebih dahulu melalui X-School-ID");
    }
}
