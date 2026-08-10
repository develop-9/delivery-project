package com.delivery_project.slack_service.global.security;

import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey secretKey;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret이 비어 있습니다. 루트 .env의 JWT_SECRET을 user-service와 같은 값으로 채워주세요."
            );
        }

        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );
    }

    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {

            throw new BusinessException(
                    ErrorCode.AUTH_UNAUTHORIZED
            );
        }

        return authorizationHeader.substring(
                BEARER_PREFIX.length()
        );
    }

    public JwtPrincipal parse(String token) {
        Claims claims = parseClaims(token);

        UUID userId =
                UUID.fromString(claims.getSubject());

        String roleClaim =
                claims.get(
                        ROLE_CLAIM,
                        String.class
                );

        return new JwtPrincipal(
                userId,
                roleClaim == null
                        ? null
                        : toRole(roleClaim)
        );
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException exception) {
            throw new BusinessException(
                    ErrorCode.AUTH_TOKEN_EXPIRED
            );

        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.AUTH_TOKEN_INVALID
            );
        }
    }

    private Role toRole(String roleClaim) {
        try {
            return Role.valueOf(roleClaim);

        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.AUTH_TOKEN_INVALID
            );
        }
    }
}