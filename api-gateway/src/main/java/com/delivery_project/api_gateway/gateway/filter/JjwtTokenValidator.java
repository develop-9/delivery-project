package com.delivery_project.api_gateway.gateway.filter;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JjwtTokenValidator implements TokenValidator {

	private final SecretKey jwtSecretKey;

	@Override
	public ValidatedToken validate(String token) {
		Claims claims;
		try {
			claims = Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			throw new ExpiredTokenException();
		} catch (JwtException | IllegalArgumentException e) {
			throw new InvalidTokenException();
		}

		String userId = claims.getSubject();
		if (userId == null || claims.getIssuedAt() == null) {
			throw new InvalidTokenException();
		}

		return new ValidatedToken(userId, claims.getIssuedAt().getTime());
	}
}
