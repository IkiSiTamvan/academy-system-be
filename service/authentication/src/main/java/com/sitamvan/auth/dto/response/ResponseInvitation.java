package com.sitamvan.auth.dto.response;

import java.time.Instant;

public record ResponseInvitation(String email, String schoolName, String roleCode, String invitationToken,
                                 Instant expiresAt) { }
