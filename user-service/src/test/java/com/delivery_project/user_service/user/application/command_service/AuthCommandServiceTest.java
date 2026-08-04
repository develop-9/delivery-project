package com.delivery_project.user_service.user.application.command_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtProvider jwtProvider;

	@InjectMocks
	private AuthCommandService authCommandService;

	@Test
	void 정상_회원가입시_비밀번호가_암호화되어_저장되고_PENDING_상태로_결과가_반환된다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, UUID.randomUUID());
		User saved = createUser(command);

		when(userRepository.existsByUsername("kim123")).thenReturn(false);
		when(userRepository.existsBySlackId("U0123456789")).thenReturn(false);
		when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenReturn(saved);

		// when
		UserSignupResult result = authCommandService.signup(command);

		// then
		assertThat(result.userId()).isEqualTo(saved.getId());
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
	}

	@Test
	void username이_중복이면_USER_DUPLICATE_USERNAME_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, UUID.randomUUID());

		when(userRepository.existsByUsername("kim123")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_USERNAME);
	}

	@Test
	void slackId가_중복이면_USER_DUPLICATE_SLACK_ID_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, UUID.randomUUID());

		when(userRepository.existsByUsername("kim123")).thenReturn(false);
		when(userRepository.existsBySlackId("U0123456789")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_SLACK_ID);
	}

	@Test
	void HUB_MANAGER인데_hubId가_없으면_INVALID_INPUT_VALUE_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.HUB_MANAGER, null, null);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void COMPANY_MANAGER인데_companyId가_없으면_INVALID_INPUT_VALUE_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, null);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void MASTER는_hubId_companyId_없이도_회원가입할_수_있다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"master1", "Abcd1234!", "관리자", "U0000000000",
				Role.MASTER, null, null);
		User saved = createUser(command);

		when(userRepository.existsByUsername("master1")).thenReturn(false);
		when(userRepository.existsBySlackId("U0000000000")).thenReturn(false);
		when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenReturn(saved);

		// when
		UserSignupResult result = authCommandService.signup(command);

		// then
		assertThat(result.userId()).isEqualTo(saved.getId());
	}

	@Test
	void 로그인_성공시_토큰을_발급하고_RefreshToken을_저장한다() {
		// given
		UserLoginCommand command = new UserLoginCommand("kim123", "Abcd1234!");
		User approvedUser = createApprovedUser("kim123", "encoded-password", Role.COMPANY_MANAGER);

		when(userRepository.findByUsername("kim123")).thenReturn(Optional.of(approvedUser));
		when(passwordEncoder.matches("Abcd1234!", "encoded-password")).thenReturn(true);
		when(jwtProvider.generateAccessToken(approvedUser.getId(), Role.COMPANY_MANAGER)).thenReturn("access-token");
		when(jwtProvider.generateRefreshToken(approvedUser.getId())).thenReturn("refresh-token");
		when(jwtProvider.getRefreshTokenExpirationMillis()).thenReturn(1_209_600_000L);
		when(jwtProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

		// when
		UserLoginResult result = authCommandService.login(command);

		// then
		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(result.expiresIn()).isEqualTo(3600L);
		org.mockito.Mockito.verify(refreshTokenRepository)
				.save(approvedUser.getId(), "refresh-token", Duration.ofMillis(1_209_600_000L));
	}

	@Test
	void 존재하지_않는_username으로_로그인하면_AUTH_INVALID_CREDENTIALS_예외가_발생한다() {
		// given
		UserLoginCommand command = new UserLoginCommand("no-such-user", "Abcd1234!");

		when(userRepository.findByUsername("no-such-user")).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authCommandService.login(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
	}

	@Test
	void 비밀번호가_틀리면_AUTH_INVALID_CREDENTIALS_예외가_발생한다() {
		// given
		UserLoginCommand command = new UserLoginCommand("kim123", "wrong-password");
		User approvedUser = createApprovedUser("kim123", "encoded-password", Role.COMPANY_MANAGER);

		when(userRepository.findByUsername("kim123")).thenReturn(Optional.of(approvedUser));
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> authCommandService.login(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
	}

	@Test
	void 승인되지_않은_사용자는_USER_NOT_APPROVED_예외가_발생한다() {
		// given
		UserLoginCommand command = new UserLoginCommand("pendinguser", "Abcd1234!");
		User pendingUser = User.builder()
				.username("pendinguser")
				.password("encoded-password")
				.name("대기자")
				.slackId("U1234567890")
				.role(Role.COMPANY_MANAGER)
				.companyId(UUID.randomUUID())
				.build();

		when(userRepository.findByUsername("pendinguser")).thenReturn(Optional.of(pendingUser));
		when(passwordEncoder.matches("Abcd1234!", "encoded-password")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authCommandService.login(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_APPROVED);
	}

	@Test
	void 정상_토큰_재발급시_새_토큰_쌍을_발급하고_RefreshToken을_교체한다() {
		// given
		UUID userId = UUID.randomUUID();
		User approvedUser = createApprovedUser("kim123", "encoded-password", Role.COMPANY_MANAGER);
		ReflectionTestUtils.setField(approvedUser, "id", userId);
		UserRefreshCommand command = new UserRefreshCommand("old-refresh-token");

		when(jwtProvider.getUserId("old-refresh-token")).thenReturn(userId);
		when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of("old-refresh-token"));
		when(userRepository.findById(userId)).thenReturn(Optional.of(approvedUser));
		when(jwtProvider.generateAccessToken(userId, Role.COMPANY_MANAGER)).thenReturn("new-access-token");
		when(jwtProvider.generateRefreshToken(userId)).thenReturn("new-refresh-token");
		when(jwtProvider.getRefreshTokenExpirationMillis()).thenReturn(1_209_600_000L);
		when(jwtProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

		// when
		UserRefreshResult result = authCommandService.refresh(command);

		// then
		assertThat(result.accessToken()).isEqualTo("new-access-token");
		assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
		org.mockito.Mockito.verify(refreshTokenRepository)
				.save(userId, "new-refresh-token", Duration.ofMillis(1_209_600_000L));
	}

	@Test
	void 저장된_RefreshToken과_다르면_AUTH_TOKEN_EXPIRED_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UserRefreshCommand command = new UserRefreshCommand("stolen-old-token");

		when(jwtProvider.getUserId("stolen-old-token")).thenReturn(userId);
		when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of("current-token"));

		// when & then
		assertThatThrownBy(() -> authCommandService.refresh(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
	}

	@Test
	void Redis에_RefreshToken이_없으면_AUTH_TOKEN_EXPIRED_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UserRefreshCommand command = new UserRefreshCommand("some-token");

		when(jwtProvider.getUserId("some-token")).thenReturn(userId);
		when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authCommandService.refresh(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
	}

	@Test
	void 재발급시_사용자를_찾을_수_없으면_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		UserRefreshCommand command = new UserRefreshCommand("valid-token");

		when(jwtProvider.getUserId("valid-token")).thenReturn(userId);
		when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of("valid-token"));
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authCommandService.refresh(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	@Test
	void 승인되지_않은_사용자는_재발급시_USER_NOT_APPROVED_예외가_발생한다() {
		// given
		UUID userId = UUID.randomUUID();
		User pendingUser = User.builder()
				.username("pendinguser2")
				.password("encoded-password")
				.name("대기자")
				.slackId("U2234567890")
				.role(Role.COMPANY_MANAGER)
				.companyId(UUID.randomUUID())
				.build();
		ReflectionTestUtils.setField(pendingUser, "id", userId);
		UserRefreshCommand command = new UserRefreshCommand("valid-token");

		when(jwtProvider.getUserId("valid-token")).thenReturn(userId);
		when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of("valid-token"));
		when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));

		// when & then
		assertThatThrownBy(() -> authCommandService.refresh(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_APPROVED);
	}

	@Test
	void 정상_로그아웃시_RefreshToken을_삭제한다() {
		// given
		UUID userId = UUID.randomUUID();
		when(jwtProvider.resolveToken("Bearer access-token")).thenReturn("access-token");
		when(jwtProvider.getUserId("access-token")).thenReturn(userId);

		// when
		authCommandService.logout("Bearer access-token");

		// then
		org.mockito.Mockito.verify(refreshTokenRepository).deleteByUserId(userId);
	}

	@Test
	void Authorization_헤더가_없으면_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		when(jwtProvider.resolveToken(null)).thenThrow(new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

		// when & then
		assertThatThrownBy(() -> authCommandService.logout(null))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	private User createApprovedUser(String username, String encodedPassword, Role role) {
		User user = User.builder()
				.username(username)
				.password(encodedPassword)
				.name("테스트유저")
				.slackId("U" + UUID.randomUUID().toString().substring(0, 10))
				.role(role)
				.companyId(UUID.randomUUID())
				.build();
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(user, "approvalStatus", ApprovalStatus.APPROVED);
		return user;
	}

	private User createUser(UserSignupCommand command) {
		return User.builder()
				.username(command.username())
				.password("encoded-password")
				.name(command.name())
				.slackId(command.slackId())
				.role(command.role())
				.hubId(command.hubId())
				.companyId(command.companyId())
				.build();
	}
}
