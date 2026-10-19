package com.sitamvan.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academy.bootstrap.platform-admin")
public record BootstrapAdminProperties(String username, String email, String password) { }
