package com.delivery_project.user_service.user.application.command_service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.global.security.JwtPrincipal;
import com.delivery_project.user_service.global.security.TokenType;
import com.delivery_project.user_service.user.application.command.UserLoginCommand;
import com.delivery_project.user_service.user.application.command.UserRefreshCommand;
import com.delivery_project.user_service.user.application.command.UserSignupCommand;
import com.delivery_project.user_service.user.application.port.TokenProvider;
import com.delivery_project.user_service.user.application.result.UserLoginResult;
import com.delivery_project.user_service.user.application.result.UserRefreshResult;
import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
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
	private final PasswordEncoder passwordEncoder;
	private final TokenProvider tokenProvider;

	public UserSignupResult signup(UserSignupCommand command) {
		log.info("[Auth] 회원가입 시도 username={}", command.username());

		validateHubOrCompanyRequired(command);

		if (userCommandRepository.existsByUsername(command.username())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_USERNAME);
		}
		if (userCommandRepository.existsBySlackId(command.slackId())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
		}

		// TODO: hubId/companyId 존재 검증 (Hub/Company Internal API 연동 후 추가, 현재는 입력값 그대로 저장)

		User user = User.builder()
				.username(command.username())
				.password(passwordEncoder.encode(command.password()))
				.name(command.name())
				.slackId(command.slackId())
				.role(command.role())
				.hubId(command.hubId())
				.companyId(command.companyId())
				.build();

		// 활성 MASTER가 한 명도 없으면 아무도 승인해줄 수 없어 첫 MASTER 가입자가 영구히
		// PENDING에 머무르는 부트스트랩 데드락이 생긴다. 이 경우에 한해 가입과 동시에 자동 승인한다.
		if (command.role() == Role.MASTER && userCommandRepository.countActiveMasters() == 0) {
			user.approveAsInitialMaster();
		}

		User saved = saveUser(user);
		log.info("[Auth] 회원가입 완료 userId={}", saved.getId());

		return UserSignupResult.from(saved);
	}

	/**
	 * existsByUsername/existsBySlackId 사전 체크와 저장 사이에 동시에 같은 값으로 가입 요청이
	 * 들어오면, 사전 체크를 통과하고도 DB의 부분 유니크 인덱스(삭제되지 않은 행에만 적용 —
	 * User.java, UserTableSchemaInitializer 참고)에서 걸릴 수 있다. 이 경우를 여기서 구체적인
	 * ErrorCode로 변환한다(그 외 제약 위반은 GlobalExceptionHandler의 일반 처리로 위임).
	 *
	 * 삭제된 사용자와 같은 username/slackId로 재가입하는 것은 더 이상 막히지 않는다 — 부분
	 * 유니크 인덱스가 삭제되지 않은 행끼리만 유일성을 검사하기 때문이다.
	 */
	private User saveUser(User user) {
		try {
			return userCommandRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			String message = e.getMessage();
			if (message != null && message.contains("(username)")) {
				throw new BusinessException(ErrorCode.USER_DUPLICATE_USERNAME);
			}
			if (message != null && message.contains("(slack_id)")) {
				throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
			}
			throw e;
		}
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

	private void validateHubOrCompanyRequired(UserSignupCommand command) {
		Role role = command.role();

		if ((role == Role.HUB_MANAGER || role == Role.DELIVERY_MANAGER) && command.hubId() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (role == Role.COMPANY_MANAGER && command.companyId() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	private record TokenPair(String accessToken, String refreshToken) {
	}
}
