package com.delivery_project.api_gateway.gateway.filter;

/**
 * JWT의 서명·만료를 검증한다. 구체적인 토큰 라이브러리는 구현체가 감추고, 호출부는
 * ValidatedToken 또는 InvalidTokenException/ExpiredTokenException만 안다.
 */
public interface TokenValidator {

	ValidatedToken validate(String token);
}
