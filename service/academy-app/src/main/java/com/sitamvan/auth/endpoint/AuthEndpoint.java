package com.sitamvan.auth.endpoint;

import com.sitamvan.auth.dto.request.RequestLogin;
import com.sitamvan.auth.dto.request.RequestAcceptInvitation;
import com.sitamvan.auth.dto.response.ResponseAuth;
import com.sitamvan.auth.dto.response.ResponseCurrentUser;
import com.sitamvan.auth.dto.response.ResponseInvitationAccepted;
import com.sitamvan.auth.security.UserPrincipal;
import com.sitamvan.auth.service.AuthenticationService;
import com.sitamvan.auth.service.SchoolAdministrationService;
import com.sitamvan.shared.data.base.BaseResponse;
import com.sitamvan.shared.data.util.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthEndpoint {
    private final AuthenticationService authenticationService;
    private final SchoolAdministrationService schoolAdministrationService;

    @PostMapping("/login")
    public BaseResponse<ResponseAuth> login(@Valid @RequestBody RequestLogin request) {
        return ResponseBuilder.ok(authenticationService.login(request.username(), request.password()), "Login successful");
    }

    @PostMapping("/invitations/accept")
    public BaseResponse<ResponseInvitationAccepted> acceptInvitation(@Valid @RequestBody RequestAcceptInvitation request) {
        return ResponseBuilder.ok(schoolAdministrationService.acceptInvitation(request), "Invitation accepted successfully");
    }

    @GetMapping("/me")
    public BaseResponse<ResponseCurrentUser> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseBuilder.ok(authenticationService.currentUser(principal.userId()), "User context fetched successfully");
    }
}
