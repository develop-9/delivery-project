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

		User saved = saveUser(user);
		log.info("[Auth] 회원가입 완료 userId={}", saved.getId());

		return UserSignupResult.from(saved);
	}

	/**
	 * existsByUsername/existsBySlackId는 @SQLRestriction으로 소프트 삭제된 행을 걸러내기 때문에,
	 * 삭제된 사용자와 같은 username/slackId로 재가입하거나 동시에 같은 값으로 가입 요청이 들어오면
	 * 사전 체크를 통과하고도 DB의 UNIQUE 제약에서 걸릴 수 있다. 이 경우를 여기서 구체적인
	 * ErrorCode로 변환한다(그 외 제약 위반은 GlobalExceptionHandler의 일반 처리로 위임).
	 *
	 * 삭제된 사용자의 username/slackId를 영구히 재사용 못 하는 게 현재 의도된 동작이다.
	 * TODO: 추후 스케줄러로 일정 기간(보관 기간 미정) 지난 소프트 삭제 행을 완전히 제거해서
	 *       재가입을 허용하는 방향 검토
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

	@Transactional(readOnly = true)
	public void logout(UUID callerId) {
		refreshTokenRepository.deleteByUserId(callerId);
		// 로그아웃도 사용자가 명시적으로 세션을 끝내려는 의도이므로, 삭제 때와 같은 기준으로
		// 이미 발급된 Access Token까지 막는다(Gateway JWT 인증 필터가 이 값과 iat를 비교).
		// 이 메서드는 DB 쓰기가 없는 읽기 전용 트랜잭션이라 롤백으로 무효화 시각만 앞서가는
		// 문제가 없으므로, delete()와 달리 커밋 이후로 미룰 필요가 없다.
		userInvalidationRepository.invalidate(callerId, Instant.now());
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
