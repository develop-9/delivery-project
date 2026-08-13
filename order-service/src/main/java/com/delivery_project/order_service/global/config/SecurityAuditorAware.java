package com.delivery_project.order_service.global.config;

import com.delivery_project.order_service.global.security.JwtAuthenticationFilter;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * {@code created_by} / {@code updated_by} 를 채우는 감사자 공급자.
 *
 * <p>공통 파일의 {@code JpaConfig} 는 {@code @EnableJpaAuditing} 뿐이라
 * {@code AuditorAware} 빈이 없으면 {@code @CreatedBy} 가 값을 채우지 않는다.
 * 두 컬럼은 {@code nullable = false} 이므로 이 빈이 없으면 INSERT 가 항상 실패한다.
 *
 * <p>인증 주체가 없으면 팀 공통 시스템 주체 ID 로 대체한다. 값은 루트 {@code .env} 의
 * {@code SYSTEM_ID} → {@code application.yaml} 의 {@code system.id} 로 주입받는다
 * (user-service 와 동일). 전 서비스가 같은 값을 써야 해서 코드에 박지 않는다.
 */
@Slf4j
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

	private final UUID systemId;

	public SecurityAuditorAware(@Value("${system.id}") UUID systemId) {
		this.systemId = systemId;
	}

	/*
	 * JwtAuthenticationFilter가 SecurityContext에 넣는 principal은 UUID 문자열이 아니라
	 * JwtPrincipal 레코드 전체다(role 기반 인가를 위해 @AuthenticationPrincipal JwtPrincipal로
	 * 컨트롤러에서 그대로 꺼내 쓰기 때문). authentication.getName()은 이 경우 principal의
	 * toString()을 반환해서 UUID 파싱이 항상 실패하므로, getPrincipal()을 직접 타입 체크한다.
	 */
	@Override
	public Optional<UUID> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.of(systemId);
		}

		return Optional.of(resolveCallerId(authentication));
	}

	/**
	 * principal 에서 사용자 ID 를 꺼낸다.
	 *
	 * <p>{@code authentication.getName()} 을 쓰지 않는다. 그 메서드는 principal 이
	 * {@code UserDetails} 나 {@code String} 이 아니면 {@code toString()} 을 돌려주는데,
	 * {@link JwtAuthenticationFilter} 는 {@link JwtPrincipal} <b>객체</b>를 넣기 때문에
	 * {@code JwtPrincipal[userId=..., role=...]} 같은 문자열이 넘어온다.
	 * 그것을 UUID 로 파싱하면 인증이 정상이어도 매번 실패해 <b>모든 행의 작성자가 시스템 ID</b> 가 된다.
	 *
	 * <p>문자열로 만들었다 다시 파싱하는 우회를 없애고 principal 에서 직접 꺼낸다.
	 * 이렇게 하면 record 필드가 늘거나 순서가 바뀌어도 영향을 받지 않는다.
	 */
	private UUID resolveCallerId(Authentication authentication) {
		Object principal = authentication.getPrincipal();

		if (principal instanceof JwtPrincipal jwtPrincipal) {
			return jwtPrincipal.userId();
		}

		log.warn("[Audit] 알 수 없는 principal 타입이라 시스템 ID 로 대체한다 : {}",
				principal == null ? "null" : principal.getClass().getName());
		return systemId;
	}
}
