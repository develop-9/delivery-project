package com.delivery_project.user_service.user.application.command_service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.global.security.TokenType;
import com.delivery_project.user_service.user.application.command.UserLoginCommand;
import com.delivery_project.user_service.user.application.command.UserRefreshCommand;
import com.delivery_project.user_service.user.application.command.UserSignupCommand;
import com.delivery_project.user_service.user.application.persistence_service.UserPersistenceService;
import com.delivery_project.user_service.user.application.port.CompanyPort;
import com.delivery_project.user_service.user.application.port.HubPort;
import com.delivery_project.user_service.user.application.port.TokenProvider;
import com.delivery_project.user_service.user.application.result.UserLoginResult;
import com.delivery_project.user_service.user.application.result.UserRefreshResult;
import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

	private final UserCommandRepository userCommandRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserPersistenceService userPersistenceService;
	private final PasswordEncoder passwordEncoder;
	private final TokenProvider tokenProvider;
	private final HubPort hubPort;
	private final CompanyPort companyPort;

	/**
	 * 클래스 레벨 @Transactional을 이 메서드에서만 명시적으로 비활성화한다(NOT_SUPPORTED).
	 * hubId/companyId 존재 검증(Hub/Company Service Feign 호출)이 DB 트랜잭션 밖에서 실행되도록
	 * 하기 위함이다 — UserCommandService.delete()와 같은 이유다. Hub/Company Service가 느리거나
	 * 다운되면 DB 커넥션을 붙잡은 채로 대기하게 되어 커넥션 풀 고갈로 이어질 수 있다.
	 *
	 * 존재 검증을 DB 쓰기보다 먼저 수행하는 것도 그대로다 — 존재하지 않는 허브/업체로 가입을
	 * 시도하는 실패 케이스에서 불필요한 DB 쓰기를 막기 위함이다.
	 *
	 * 실제 DB 저장(및 MASTER 최초 부트스트랩 처리)은 userPersistenceService.commitSignup()의
	 * 별도 트랜잭션에서 이뤄진다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public UserSignupResult signup(UserSignupCommand command) {
		log.info("[Auth] 회원가입 시도 username={}", command.username());

		if (userCommandRepository.existsByUsername(command.username())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_USERNAME);
		}
		if (userCommandRepository.existsBySlackId(command.slackId())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
		}

		if (command.hubId() != null) {
			hubPort.validateExists(command.hubId());
		}
		if (command.companyId() != null) {
			companyPort.validateExists(command.companyId());
		}

		User user = User.builder()
				.username(command.username())
				.password(passwordEncoder.encode(command.password()))
				.name(command.name())
				.slackId(command.slackId())
				.role(command.role())
				.hubId(command.hubId())
				.companyId(command.companyId())
				.build();

		User saved = userPersistenceService.commitSignup(user);
		log.info("[Auth] 회원가입 완료 userId={}", saved.getId());

		return UserSignupResult.from(saved);
	}

	@Transactional(readOnly = true)
	public UserLoginResult login(UserLoginCommand command) {
		log.info("[Auth] 로그인 시도 username={}", command.username());

		User user = userCommandRepository.findByUsername(command.username())
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(command.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}
		if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
		}

		UUID sessionId = UUID.randomUUID();
		TokenPair tokens = issueTokens(user, sessionId);
		log.info("[Auth] 로그인 성공 userId={} sessionId={}", user.getId(), sessionId);

		return new UserLoginResult(tokens.accessToken(), tokens.refreshToken(), tokenProvider.getAccessTokenExpirationSeconds());
	}

	@Transactional(readOnly = true)
	public UserRefreshResult refresh(UserRefreshCommand command) {
		log.info("[Auth] 토큰 재발급 시도");

		String requestedRefreshToken = command.refreshToken();
		// Access Token은 refreshSecretKey로 서명되지 않았으므로 여기서 서명 검증 단계부터 실패한다.
		// tokenType 체크는 두 시크릿이 실수로 같아지는 설정 오류에 대비한 이중 방어다.
		JwtPrincipal principal = tokenProvider.parseRefreshToken(requestedRefreshToken);
		if (principal.tokenType() != TokenType.REFRESH) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		UUID userId = principal.userId();
		// 로테이션돼도 같은 기기의 같은 세션임을 유지하기 위해 sessionId는 새로 만들지 않고 그대로 재사용한다.
		UUID sessionId = principal.sessionId();

		User user = userCommandRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
		if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
		}

		// 저장된 값과의 비교와 교체를 Redis 쪽에서 원자적으로 한 번에 처리한다(compareAndRotate).
		// 조회 후 애플리케이션에서 비교하고 따로 저장하는 방식은 그 사이에 동시 재발급 요청이
		// 끼어들 여지가 있어서, 같은 refresh token으로 동시에 두 번 요청이 와도 하나만 성공하게 한다.
		String newRefreshToken = tokenProvider.generateRefreshToken(userId, sessionId);
		boolean rotated = refreshTokenRepository.compareAndRotate(
				userId, sessionId, requestedRefreshToken, newRefreshToken,
				Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMillis()));
		if (!rotated) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
		}
		String accessToken = tokenProvider.generateAccessToken(userId, user.getRole(), sessionId);

		log.info("[Auth] 토큰 재발급 성공 userId={} sessionId={}", userId, sessionId);

		return new UserRefreshResult(accessToken, newRefreshToken, tokenProvider.getAccessTokenExpirationSeconds());
	}

	/**
	 * DB 쓰기가 없는 메서드라 signup()/delete()와 같은 이유로 클래스 레벨 @Transactional을
	 * 비활성화한다 — Redis 호출만 하면서 굳이 DB 커넥션을 붙잡고 있을 이유가 없다.
	 *
	 * 로그아웃을 요청한 기기의 세션 하나만 끝낸다(다른 기기의 세션은 그대로 유지 — 다중 기기 지원).
	 * 블랙리스트 등록을 세션 삭제보다 먼저 하는 이유: 블랙리스트는 sessionId 단위로 걸리므로,
	 * 뒤이은 삭제가 실패해서 이 refresh token으로 재발급을 한 번 더 받아내더라도 같은 sessionId를
	 * 물려받은 새 Access Token까지 Gateway에서 그대로 막힌다 — 삭제 실패의 피해 범위가 더 좁다.
	 * 정지/삭제와 달리 로그아웃한 사용자는 여전히 APPROVED라 refresh()의 승인 상태 재검증으로
	 * 방어가 안 되므로, 두 호출 모두 Redis 실패를 성공으로 위장하지 않고 그대로 던진다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void logout(UUID callerId, String authorizationHeader) {
		String accessToken = tokenProvider.resolveToken(authorizationHeader);
		JwtPrincipal principal = tokenProvider.parseAccessToken(accessToken);
		UUID sessionId = principal.sessionId();

		refreshTokenRepository.blacklistSessionOrThrow(
				callerId, sessionId, Duration.ofSeconds(tokenProvider.getAccessTokenExpirationSeconds()));
		refreshTokenRepository.deleteByUserIdAndSessionIdOrThrow(callerId, sessionId);

		log.info("[Auth] 로그아웃 완료 userId={} sessionId={}", callerId, sessionId);
	}

	private TokenPair issueTokens(User user, UUID sessionId) {
		String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole(), sessionId);
		String refreshToken = tokenProvider.generateRefreshToken(user.getId(), sessionId);
		refreshTokenRepository.save(user.getId(), sessionId, refreshToken,
				Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMillis()));
		return new TokenPair(accessToken, refreshToken);
	}

	private record TokenPair(String accessToken, String refreshToken) {
	}
}
