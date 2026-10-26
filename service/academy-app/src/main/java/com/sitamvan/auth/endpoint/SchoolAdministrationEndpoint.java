package com.sitamvan.auth.endpoint;

import com.sitamvan.academic.model.School;
import com.sitamvan.auth.dto.request.RequestCreateInvitation;
import com.sitamvan.auth.dto.request.RequestCreateSchool;
import com.sitamvan.auth.dto.response.ResponseInvitation;
import com.sitamvan.auth.dto.response.ResponseSchool;
import com.sitamvan.auth.service.SchoolAdministrationService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.sitamvan.auth.security.UserPrincipal;
import java.util.List;

@RestController
@RequestMapping("/v1/schools")
@RequiredArgsConstructor
public class SchoolAdministrationEndpoint {
    private final SchoolAdministrationService service;

    @GetMapping
    public BaseResponse<List<ResponseSchool>> listSchools(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseBuilder.ok(service.listSchools(principal.userId()).stream()
                .map(school -> new ResponseSchool(school.getId(), school.getCode(), school.getName(), school.getTimeZone()))
                .toList(), "Sekolah dimuat");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public BaseResponse<ResponseSchool> createSchool(@Valid @RequestBody RequestCreateSchool request) {
        School school = service.createSchool(request);
        return ResponseBuilder.ok(new ResponseSchool(school.getId(), school.getCode(), school.getName(), school.getTimeZone()), "School created successfully");
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAnyAuthority('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public BaseResponse<ResponseInvitation> createInvitation(@RequestHeader("X-School-ID") Long schoolId,
                                                              @Valid @RequestBody RequestCreateInvitation request) {
        return ResponseBuilder.ok(service.createInvitation(schoolId, request), "Invitation created successfully");
    }
}
