package com.sitamvan.academic.dto.response;

import java.time.Instant;

public record ResponseTeacher(Long id, String employeeCode, String name, String email,
                              String phone, boolean active, boolean external,
                              Instant createdAt, Instant modifiedAt) { }
