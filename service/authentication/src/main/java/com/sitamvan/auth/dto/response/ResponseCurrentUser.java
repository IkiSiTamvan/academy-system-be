package com.sitamvan.auth.dto.response;

import java.util.List;

public record ResponseCurrentUser(Long id, String username, String email, List<String> globalRoles,
                                 List<ResponseMembership> memberships) { }
