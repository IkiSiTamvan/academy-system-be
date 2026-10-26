package com.sitamvan.auth.security;

import com.sitamvan.auth.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    @Test
    void issuedTokenContainsUserIdentityAndCanBeParsed() {
        JwtService jwtService = new JwtService(new JwtProperties("01234567890123456789012345678901", 86_400_000));

        String token = jwtService.issue(42L);

        assertThat(jwtService.parseUserId(token)).isEqualTo(42L);
        assertThat(jwtService.expiresInSeconds()).isEqualTo(86_400L);
    }

    @Test
    void invalidSignatureIsRejected() {
        JwtService issuer = new JwtService(new JwtProperties("01234567890123456789012345678901", 86_400_000));
        JwtService verifier = new JwtService(new JwtProperties("abcdefghijklmnopqrstuvwxyz123456", 86_400_000));

        assertThatThrownBy(() -> verifier.parseUserId(issuer.issue(42L)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void secretMustBeAtLeastThirtyTwoBytes() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 60_000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
