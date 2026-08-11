package com.delivery_project.user_service.user.application.command_service;

import java.time.Duration;
import java.time.Instant;
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
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

	private final UserCommandRepository userCommandRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserInvalidationRepository userInvalidationRepository;
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

		TokenPair tokens = issueTokens(user);
		log.info("[Auth] 로그인 성공 userId={}", user.getId());

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

		String storedRefreshToken = refreshTokenRepository.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED));
		if (!storedRefreshToken.equals(requestedRefreshToken)) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
		}

		User user = userCommandRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
		if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
		}

		TokenPair tokens = issueTokens(user);
		log.info("[Auth] 토큰 재발급 성공 userId={}", userId);

		return new UserRefreshResult(tokens.accessToken(), tokens.refreshToken(), tokenProvider.getAccessTokenExpirationSeconds());
	}

	/**
	 * Access Token 무효화 기록을 Refresh Token 삭제보다 먼저 한다.
	 * 아웃박스 DB insert라 Redis 상태와 무관하게 항상 성공하므로,
	 * 뒤이은 Refresh Token 삭제가 실패해도 이 기록만은 반드시 남아야 한다.
	 * noRollbackFor로 그 실패가 이 기록까지 롤백시키지 않게 막는다.
	 */
	@Transactional(noRollbackFor = IllegalStateException.class)
	public void logout(UUID callerId) {
		// 로그아웃도 사용자가 명시적으로 세션을 끝내려는 의도이므로, 삭제 때와 같은 기준으로
		// 이미 발급된 Access Token까지 막는다(Gateway JWT 인증 필터가 이 값과 iat를 비교).
		userInvalidationRepository.invalidate(callerId, Instant.now());

		// 정지/삭제와 달리 로그아웃한 사용자는 여전히 APPROVED라 refresh()의 승인 상태
		// 재검증으로 방어가 안 된다 — Redis 삭제가 실패해도 성공으로 위장하면 안 되므로
		// fail-open인 deleteByUserId() 대신 실패를 그대로 던지는 쪽을 쓴다.
		refreshTokenRepository.deleteByUserIdOrThrow(callerId);

		log.info("[Auth] 로그아웃 완료 userId={}", callerId);
	}

	private TokenPair issueTokens(User user) {
		String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
		String refreshToken = tokenProvider.generateRefreshToken(user.getId());
		refreshTokenRepository.save(user.getId(), refreshToken, Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMillis()));
		return new TokenPair(accessToken, refreshToken);
	}

	private record TokenPair(String accessToken, String refreshToken) {
	}
}
