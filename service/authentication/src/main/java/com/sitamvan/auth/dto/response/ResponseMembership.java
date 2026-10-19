package com.sitamvan.auth.dto.response;

import java.util.List;

public record ResponseMembership(Long schoolId, String schoolCode, String schoolName, List<String> roles) { }
