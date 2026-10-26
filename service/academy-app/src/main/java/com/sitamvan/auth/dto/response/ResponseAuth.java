package com.sitamvan.auth.dto.response;

public record ResponseAuth(String accessToken, String tokenType, long expiresInSeconds) { }
