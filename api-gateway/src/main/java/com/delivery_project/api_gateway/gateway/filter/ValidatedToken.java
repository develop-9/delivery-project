package com.delivery_project.api_gateway.gateway.filter;

public record ValidatedToken(
		String userId,
		long issuedAtMillis,
		String sessionId
) {
}
