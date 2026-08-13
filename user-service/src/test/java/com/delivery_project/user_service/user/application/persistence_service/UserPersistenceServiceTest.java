package com.delivery_project.user_service.user.application.persistence_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.port.MasterBootstrapLockPort;
import com.delivery_project.user_service.user.application.result.UserDeleteResult;
import com.delivery_project.user_service.user.application.result.UserReinstateResult;
import com.delivery_project.user_service.user.application.result.UserSuspendResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

@ExtendWith(MockitoExtension.class)
class UserPersistenceServiceTest {

	@Mock
	private UserCommandRepository userCommandRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private UserInvalidationRepository userInvalidationRepository;

	@Mock
	private MasterBootstrapLockPort masterBootstrapLockPort;

	@InjectMocks
	private UserPersistenceService userPersistenceService;

	@Test
	void 활성_MASTER가_없으면_MASTER_가입시_자동으로_승인된다() {
		// given
		User user = createUser("master1", Role.MASTER, null);

		when(userCommandRepository.countActiveMasters()).thenReturn(0L);
		when(userCommandRepository.save(user)).thenReturn(user);

		// when
		User saved = userPersistenceService.commitSignup(user);

		// then
		assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
		verify(masterBootstrapLockPort).lock();
	}

	@Test
	void 활성_MASTER가_이미_있으면_MASTER로_가입해도_PENDING으로_시작한다() {
		// given
		User user = createUser("master2", Role.MASTER, null);

		when(userCommandRepository.countActiveMasters()).thenReturn(1L);
		when(userCommandRepository.save(user)).thenReturn(user);

		// when
		User saved = userPersistenceService.commitSignup(user);

		// then
		assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
		verify(masterBootstrapLockPort).lock();
	}

	@Test
	void MASTER가_아니면_부트스트랩_락을_걸지_않는다() {
		// given
		User user = createUser("user1", Role.COMPANY_MANAGER, UUID.randomUUID());

		when(userCommandRepository.save(user)).thenReturn(user);

		// when
		userPersistenceService.commitSignup(user);

		// then
		verify(masterBootstrapLockPort, never()).lock();
	}

	@Test
	void 사전_중복체크는_통과했지만_저장시점에_username_제약을_위반하면_USER_DUPLICATE_USERNAME_예외가_발생한다() {
		// given: existsByUsername 사전 체크와 저장 사이에 동시에 같은 username으로 가입
		// 요청이 들어와, 사전 체크는 통과했지만 저장 시점에 부분 유니크 인덱스에 걸리는 상황을 재현
		User user = createUser("kim123", Role.COMPANY_MANAGER, UUID.randomUUID());

		when(userCommandRepository.save(user)).thenThrow(
				new DataIntegrityViolationException(
						"could not execute statement; Detail: Key (username)=(kim123) already exists."));

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitSignup(user))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_USERNAME);
	}

	@Test
	void 사전_중복체크는_통과했지만_저장시점에_slackId_제약을_위반하면_USER_DUPLICATE_SLACK_ID_예외가_발생한다() {
		// given
		User user = createUser("kim123", Role.COMPANY_MANAGER, UUID.randomUUID());

		when(userCommandRepository.save(user)).thenThrow(
				new DataIntegrityViolationException(
						"could not execute statement; Detail: Key (slack_id)=(U0123456789) already exists."));

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitSignup(user))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_SLACK_ID);
	}

	@Test
	void 알_수_없는_제약_위반은_그대로_전파되어_GlobalExceptionHandler의_일반_처리로_넘어간다() {
		// given
		User user = createUser("kim123", Role.COMPANY_MANAGER, UUID.randomUUID());

		when(userCommandRepository.save(user)).thenThrow(
				new DataIntegrityViolationException("unrelated constraint violation"));

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitSignup(user))
				.isInstanceOf(DataIntegrityViolationException.class)
				.isNotInstanceOf(BusinessException.class);
	}

	@Test
	void MASTER가_삭제되면_Soft_Delete되고_Refresh_Token도_제거된다() {
		// given
		User target = createUser("target1", Role.COMPANY_MANAGER, UUID.randomUUID());
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserDeleteResult result = userPersistenceService.commitDelete(target.getId(), UUID.randomUUID());

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(target.isDeleted()).isTrue();
		verify(refreshTokenRepository).deleteAllByUserId(target.getId());
		verify(userInvalidationRepository).invalidate(
				org.mockito.ArgumentMatchers.eq(target.getId()), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 마지막_남은_MASTER를_삭제하려하면_LAST_MASTER_DELETE_FORBIDDEN_예외가_발생한다() {
		// given
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(1L);

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitDelete(target.getId(), UUID.randomUUID()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.LAST_MASTER_DELETE_FORBIDDEN);
	}

	@Test
	void MASTER가_여러_명이면_그중_하나를_삭제할_수_있다() {
		// given
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(2L);

		// when
		UserDeleteResult result = userPersistenceService.commitDelete(target.getId(), UUID.randomUUID());

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(target.isDeleted()).isTrue();
	}

	@Test
	void 아직_승인되지_않은_MASTER_신청자는_활성_MASTER_수와_무관하게_삭제할_수_있다() {
		// given
		User pendingMaster = createUser("pending-master", Role.MASTER, null);
		when(userCommandRepository.findById(pendingMaster.getId())).thenReturn(Optional.of(pendingMaster));

		// when
		UserDeleteResult result = userPersistenceService.commitDelete(pendingMaster.getId(), UUID.randomUUID());

		// then
		assertThat(result.userId()).isEqualTo(pendingMaster.getId());
	}

	@Test
	void 삭제_대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		UUID targetId = UUID.randomUUID();
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitDelete(targetId, UUID.randomUUID()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void APPROVED_사용자를_정지하면_SUSPENDED_상태가_되고_Refresh_Token도_제거된다() {
		// given
		User target = createUser("target1", Role.COMPANY_MANAGER, UUID.randomUUID());
		target.approve(UUID.randomUUID());
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserSuspendResult result = userPersistenceService.commitSuspend(target.getId(), UUID.randomUUID());

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
		verify(refreshTokenRepository).deleteAllByUserId(target.getId());
		verify(userInvalidationRepository).invalidate(
				org.mockito.ArgumentMatchers.eq(target.getId()), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 마지막_남은_MASTER를_정지하려하면_LAST_MASTER_SUSPEND_FORBIDDEN_예외가_발생한다() {
		// given
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(1L);

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitSuspend(target.getId(), UUID.randomUUID()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.LAST_MASTER_SUSPEND_FORBIDDEN);
	}

	@Test
	void MASTER가_여러_명이면_그중_하나를_정지할_수_있다() {
		// given
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(2L);

		// when
		UserSuspendResult result = userPersistenceService.commitSuspend(target.getId(), UUID.randomUUID());

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
	}

	@Test
	void 정지_대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		UUID targetId = UUID.randomUUID();
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitSuspend(targetId, UUID.randomUUID()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void SUSPENDED_사용자를_정지_해제하면_APPROVED_상태로_돌아간다() {
		// given
		User target = createUser("target1", Role.COMPANY_MANAGER, UUID.randomUUID());
		target.approve(UUID.randomUUID());
		target.suspend();
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserReinstateResult result = userPersistenceService.commitReinstate(target.getId(), UUID.randomUUID());

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
	}

	@Test
	void 정지_해제_대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		UUID targetId = UUID.randomUUID();
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userPersistenceService.commitReinstate(targetId, UUID.randomUUID()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private User createUser(String username, Role role, UUID companyId) {
		UUID resolvedCompanyId = (role == Role.COMPANY_MANAGER && companyId == null)
				? UUID.randomUUID()
				: companyId;

		User user = User.builder()
				.username(username)
				.password("encoded-password")
				.name("테스트유저")
				.slackId("U" + UUID.randomUUID().toString().substring(0, 10))
				.role(role)
				.companyId(resolvedCompanyId)
				.build();
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}
}
