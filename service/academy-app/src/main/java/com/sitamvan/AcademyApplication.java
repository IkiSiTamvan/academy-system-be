package com.sitamvan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sitamvan.auth.security.UserPrincipal;

import java.util.Optional;

@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.sitamvan")
@EntityScan("com.sitamvan")
@EnableJpaRepositories("com.sitamvan")
@ConfigurationPropertiesScan("com.sitamvan")
public class AcademyApplication {

    @org.springframework.context.annotation.Bean
    AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(UserPrincipal.class::isInstance)
                .map(UserPrincipal.class::cast)
                .map(UserPrincipal::userId)
                .or(() -> Optional.of(0L));
    }

    public static void main(String[] args) {
        SpringApplication.run(AcademyApplication.class, args);
    }
}
