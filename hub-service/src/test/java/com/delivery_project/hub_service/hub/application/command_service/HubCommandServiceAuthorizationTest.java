package com.delivery_project.hub_service.hub.application.command_service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.delivery_project.hub_service.global.security.Role;
import com.delivery_project.hub_service.hub.application.command.HubCreateCommand;
import com.delivery_project.hub_service.hub.application.command.HubDeleteCommand;
import com.delivery_project.hub_service.hub.application.command.HubUpdateCommand;
import com.delivery_project.hub_service.hub.application.support.HubCacheEvictor;
import com.delivery_project.hub_service.hub.domain.entity.HubType;
import com.delivery_project.hub_service.hub.domain.repository.HubRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteRepository;

/**
 * 쓰기 API 의 {@code MASTER} 전용 판정 검증 ({@code @PreAuthorize}).
 *
 * <p>{@code ApiSecurityTest} 는 서비스가 대역이라 이 규칙을 태우지 못한다. 여기서는 실제 빈을 주입해
 * <b>프록시에 걸린 권한 검사</b>를 그대로 확인한다.
 *
 * <p>권한 검사는 트랜잭션보다 먼저 돈다. 그래서 거절 케이스는 DB 없이도 검증된다.
 * 반대로 통과 케이스는 트랜잭션 시작까지 가서 DB 연결에서 실패하는데, 그게 곧
 * "권한 검사를 통과했다"는 증거다 — 실패 원인이 {@link AccessDeniedException} 이 아님을 확인한다.
 * 커넥션 타임아웃을 최소값으로 낮춰 그 실패가 빨리 나게 했다.
 */
@SpringBootTest(properties = "spring.datasource.hikari.connection-timeout=250")
class HubCommandServiceAuthorizationTest {

	private static final BigDecimal LATITUDE = BigDecimal.valueOf(37.272);
	private static final BigDecimal LONGITUDE = BigDecimal.valueOf(127.435);

	@Autowired
	private HubCommandService hubCommandService;

	@MockitoBean
	private HubRepository hubRepository;

	@MockitoBean
	private HubRouteRepository hubRouteRepository;

	@MockitoBean
	private HubCacheEvictor HubCacheEvictor;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = "MASTER", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("MASTER 가 아니면 허브를 생성할 수 없다")
	void rejectsCreateForNonMasterRoles(Role role) {
		// given
		authenticateAs(role);

		// when & then
		assertThatThrownBy(() -> hubCommandService.create(UUID.randomUUID(), createCommand()))
				.isInstanceOf(AccessDeniedException.class);
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = "MASTER", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("MASTER 가 아니면 허브를 수정할 수 없다")
	void rejectsUpdateForNonMasterRoles(Role role) {
		// given
		authenticateAs(role);

		HubUpdateCommand command = new HubUpdateCommand(
				UUID.randomUUID(), "새 이름", null, null, null, null, null);

		// when & then
		assertThatThrownBy(() -> hubCommandService.update(UUID.randomUUID(), command))
				.isInstanceOf(AccessDeniedException.class);
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = "MASTER", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("MASTER 가 아니면 허브를 삭제할 수 없다")
	void rejectsDeleteForNonMasterRoles(Role role) {
		// given
		authenticateAs(role);

		// when & then
		assertThatThrownBy(() ->
				hubCommandService.delete(UUID.randomUUID(), new HubDeleteCommand(UUID.randomUUID())))
				.isInstanceOf(AccessDeniedException.class);
	}

	/**
	 * 인증 주체가 없을 때는 {@link AccessDeniedException} 이 아니라
	 * {@link AuthenticationException}({@code AuthenticationCredentialsNotFoundException}) 이 나온다.
	 * "권한이 모자란 것"과 "누구인지 모르는 것"을 Spring Security 가 구분하기 때문이다.
	 *
	 * <p>HTTP 경로에서는 필터체인이 먼저 401 을 내 여기까지 오지 않지만, 스케줄러처럼 필터를
	 * 거치지 않는 호출은 이 예외를 만난다. {@code GlobalExceptionHandler} 가 401 로 옮긴다.
	 */
	@Test
	@DisplayName("인증 정보가 없으면 권한 부족이 아니라 인증 실패로 막힌다")
	void rejectsCreateForAnonymous() {
		// given: SecurityContext 가 비어 있다

		// when & then
		assertThatThrownBy(() -> hubCommandService.create(UUID.randomUUID(), createCommand()))
				.isInstanceOf(AuthenticationException.class);
	}

	@Test
	@DisplayName("MASTER 는 권한 검사를 통과한다")
	void allowsMaster() {
		// given
		authenticateAs(Role.MASTER);

		// when & then: 권한 검사를 지나 트랜잭션까지 갔다는 뜻이라 AccessDenied 가 아니어야 한다
		assertThatThrownBy(() -> hubCommandService.create(UUID.randomUUID(), createCommand()))
				.isNotInstanceOf(AccessDeniedException.class);
	}

	private void authenticateAs(Role role) {
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, authorities));
	}

	private HubCreateCommand createCommand() {
		return new HubCreateCommand(
				"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.MAIN, null);
	}
}
