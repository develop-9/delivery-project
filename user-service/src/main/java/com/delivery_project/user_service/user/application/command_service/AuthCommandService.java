package com.delivery_project.user_service.user.application.command_service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.command.UserLoginCommand;
import com.delivery_project.user_service.user.application.command.UserRefreshCommand;
import com.delivery_project.user_service.user.application.command.UserSignupCommand;
import com.delivery_project.user_service.user.application.result.UserLoginResult;
import com.delivery_project.user_service.user.application.result.UserRefreshResult;
import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserRepository;
import com.delivery_project.user_service.user.infrastructure.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public UserSignupResult signup(UserSignupCommand command) {
		log.info("[Auth] 회원가입 시도 username={}", command.username());

		validateHubOrCompanyRequired(command);

		if (userRepository.existsByUsername(command.username())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_USERNAME);
		}
		if (userRepository.existsBySlackId(command.slackId())) {
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
			return userRepository.save(user);
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

		User user = userRepository.findByUsername(command.username())
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(command.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}
		if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
		}

		TokenPair tokens = issueTokens(user);
		log.info("[Auth] 로그인 성공 userId={}", user.getId());

		return new UserLoginResult(tokens.accessToken(), tokens.refreshToken(), jwtProvider.getAccessTokenExpirationSeconds());
	}

	@Transactional(readOnly = true)
	public UserRefreshResult refresh(UserRefreshCommand command) {
		log.info("[Auth] 토큰 재발급 시도");

		String requestedRefreshToken = command.refreshToken();
		UUID userId = jwtProvider.parse(requestedRefreshToken).userId();

		String storedRefreshToken = refreshTokenRepository.findByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED));
		if (!storedRefreshToken.equals(requestedRefreshToken)) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
		if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
		}

		TokenPair tokens = issueTokens(user);
		log.info("[Auth] 토큰 재발급 성공 userId={}", userId);

		return new UserRefreshResult(tokens.accessToken(), tokens.refreshToken(), jwtProvider.getAccessTokenExpirationSeconds());
	}

	@Transactional(readOnly = true)
	public void logout(UUID callerId) {
		refreshTokenRepository.deleteByUserId(callerId);
		log.info("[Auth] 로그아웃 완료 userId={}", callerId);
	}

	private TokenPair issueTokens(User user) {
		String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
		String refreshToken = jwtProvider.generateRefreshToken(user.getId());
		refreshTokenRepository.save(user.getId(), refreshToken, Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis()));
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
