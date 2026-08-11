package com.delivery_project.user_service.user.application.command_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.transaction.PlatformTransactionManager;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.command.UserApproveCommand;
import com.delivery_project.user_service.user.application.command.UserDeleteCommand;
import com.delivery_project.user_service.user.application.command.UserReinstateCommand;
import com.delivery_project.user_service.user.application.command.UserRejectCommand;
import com.delivery_project.user_service.user.application.command.UserSuspendCommand;
import com.delivery_project.user_service.user.application.command.UserUpdateMeCommand;
import com.delivery_project.user_service.user.application.port.DeliveryManagerPort;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserDeleteResult;
import com.delivery_project.user_service.user.application.result.UserReinstateResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.application.result.UserSuspendResult;
import com.delivery_project.user_service.user.application.result.UserUpdateMeResult;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

	@Mock
	private UserCommandRepository userCommandRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private UserInvalidationRepository userInvalidationRepository;

	@Mock
	private DeliveryManagerPort deliveryManagerPort;

	@Mock
	private CallerResolver callerResolver;

	@Mock
	private PlatformTransactionManager transactionManager;

	@InjectMocks
	private UserCommandService userCommandService;

	@Test
	void 이름과_Slack_ID를_모두_수정하면_둘_다_반영된다() {
		// given
		User caller = createUser("user1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(caller.getId())).thenReturn(caller);
		when(userCommandRepository.existsBySlackId("U9999999999")).thenReturn(false);

		// when
		UserUpdateMeResult result = userCommandService.updateMe(
				caller.getId(), new UserUpdateMeCommand("새이름", "U9999999999"));

		// then
		assertThat(result.name()).isEqualTo("새이름");
		assertThat(result.slackId()).isEqualTo("U9999999999");
	}

	@Test
	void 필드를_null로_보내면_기존_값이_유지된다() {
		// given
		User caller = createUser("user1", Role.COMPANY_MANAGER, null);
		String originalSlackId = caller.getSlackId();
		when(callerResolver.resolve(caller.getId())).thenReturn(caller);

		// when
		UserUpdateMeResult result = userCommandService.updateMe(
				caller.getId(), new UserUpdateMeCommand(null, null));

		// then
		assertThat(result.name()).isEqualTo("테스트유저");
		assertThat(result.slackId()).isEqualTo(originalSlackId);
	}

	@Test
	void 이미_사용중인_Slack_ID로_변경하려하면_USER_DUPLICATE_SLACK_ID_예외가_발생한다() {
		// given
		User caller = createUser("user1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(caller.getId())).thenReturn(caller);
		when(userCommandRepository.existsBySlackId("U1111111111")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> userCommandService.updateMe(
				caller.getId(), new UserUpdateMeCommand(null, "U1111111111")))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_SLACK_ID);
	}

	@Test
	void 본인의_기존_Slack_ID와_동일한_값으로_보내면_중복_검사를_하지_않는다() {
		// given
		User caller = createUser("user1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(caller.getId())).thenReturn(caller);

		// when
		UserUpdateMeResult result = userCommandService.updateMe(
				caller.getId(), new UserUpdateMeCommand(null, caller.getSlackId()));

		// then
		assertThat(result.slackId()).isEqualTo(caller.getSlackId());
	}

	@Test
	void 사전검사_통과_후_동시_요청으로_DB_제약이_위반되면_USER_DUPLICATE_SLACK_ID_예외가_발생한다() {
		// given
		User caller = createUser("user1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(caller.getId())).thenReturn(caller);
		when(userCommandRepository.existsBySlackId("U2222222222")).thenReturn(false);
		when(userCommandRepository.save(caller)).thenThrow(new DataIntegrityViolationException("duplicate key"));

		// when & then
		assertThatThrownBy(() -> userCommandService.updateMe(
				caller.getId(), new UserUpdateMeCommand(null, "U2222222222")))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_SLACK_ID);
	}

	@Test
	void MASTER가_삭제하면_Soft_Delete되고_Refresh_Token도_제거된다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserDeleteResult result = userCommandService.delete(master.getId(), new UserDeleteCommand(target.getId()));

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(target.isDeleted()).isTrue();
		org.mockito.Mockito.verify(refreshTokenRepository).deleteByUserId(target.getId());
		org.mockito.Mockito.verify(userInvalidationRepository).invalidate(
				org.mockito.ArgumentMatchers.eq(target.getId()), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void DELIVERY_MANAGER를_삭제하면_Delivery_Service에도_삭제를_동기화한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUserWithHub("target1", Role.DELIVERY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		userCommandService.delete(master.getId(), new UserDeleteCommand(target.getId()));

		// then
		org.mockito.Mockito.verify(deliveryManagerPort).deleteByUserId(target.getId());
	}

	/**
	 * 존재하지 않는 레코드를 멱등 성공으로 처리하는 건 DeliveryManagerFeignAdapter의 책임이라
	 * (DeliveryManagerFeignAdapterTest 참고), 여기서는 Port가 정상 반환(예외 없음)하는 경우
	 * 사용자 삭제가 그대로 진행되는지만 확인한다.
	 */
	@Test
	void Delivery_Service_연동이_성공하면_사용자_삭제가_그대로_진행된다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUserWithHub("target1", Role.DELIVERY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserDeleteResult result = userCommandService.delete(master.getId(), new UserDeleteCommand(target.getId()));

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(target.isDeleted()).isTrue();
	}

	@Test
	void Delivery_Service_연동이_실패하면_사용자_삭제_자체가_막힌다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUserWithHub("target1", Role.DELIVERY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE))
				.when(deliveryManagerPort).deleteByUserId(target.getId());

		// when & then
		assertThatThrownBy(() -> userCommandService.delete(master.getId(), new UserDeleteCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
		assertThat(target.isDeleted()).isFalse();
		org.mockito.Mockito.verify(refreshTokenRepository, org.mockito.Mockito.never()).deleteByUserId(target.getId());
		org.mockito.Mockito.verify(userInvalidationRepository, org.mockito.Mockito.never())
				.invalidate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void MASTER가_아니면_삭제_권한이_없다() {
		// given
		User hubManager = createUserWithHub("hub1", Role.HUB_MANAGER, UUID.randomUUID());
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);

		// when & then
		assertThatThrownBy(() -> userCommandService.delete(hubManager.getId(), new UserDeleteCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.DELETE_USER_FORBIDDEN);
	}

	@Test
	void 마지막_남은_MASTER를_삭제하려하면_LAST_MASTER_DELETE_FORBIDDEN_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(1L);

		// when & then
		assertThatThrownBy(() -> userCommandService.delete(master.getId(), new UserDeleteCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.LAST_MASTER_DELETE_FORBIDDEN);
	}

	@Test
	void MASTER가_여러_명이면_그중_하나를_삭제할_수_있다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(2L);

		// when
		UserDeleteResult result = userCommandService.delete(master.getId(), new UserDeleteCommand(target.getId()));

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(target.isDeleted()).isTrue();
	}

	@Test
	void 아직_승인되지_않은_MASTER_신청자는_활성_MASTER_수와_무관하게_삭제할_수_있다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User pendingMaster = createUser("pending-master", Role.MASTER, null);
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(pendingMaster.getId())).thenReturn(Optional.of(pendingMaster));

		// when
		UserDeleteResult result = userCommandService.delete(master.getId(), new UserDeleteCommand(pendingMaster.getId()));

		// then
		assertThat(result.userId()).isEqualTo(pendingMaster.getId());
	}

	@Test
	void 삭제_대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userCommandService.delete(master.getId(), new UserDeleteCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void MASTER는_아무_사용자나_승인할_수_있다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserApproveResult result = userCommandService.approve(master.getId(), new UserApproveCommand(target.getId()));

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
		assertThat(result.approvedBy()).isEqualTo(master.getId());
	}

	@Test
	void HUB_MANAGER는_같은_허브_소속_사용자를_승인할_수_있다() {
		// given
		UUID hubId = UUID.randomUUID();
		User hubManager = createUserWithHub("hub1", Role.HUB_MANAGER, hubId);
		User target = createUserWithHub("target1", Role.DELIVERY_MANAGER, hubId);
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserApproveResult result = userCommandService.approve(hubManager.getId(), new UserApproveCommand(target.getId()));

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
	}

	@Test
	void HUB_MANAGER가_다른_허브_소속_사용자를_승인하려하면_HUB_PERMISSION_DENIED_예외가_발생한다() {
		// given
		User hubManager = createUserWithHub("hub1", Role.HUB_MANAGER, UUID.randomUUID());
		User target = createUserWithHub("target1", Role.DELIVERY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(hubManager.getId(), new UserApproveCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HUB_PERMISSION_DENIED);
	}

	@Test
	void COMPANY_MANAGER는_승인_권한이_없다() {
		// given
		User companyManager = createUser("company1", Role.COMPANY_MANAGER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(companyManager.getId())).thenReturn(companyManager);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(companyManager.getId(), new UserApproveCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.APPROVE_USER_FORBIDDEN);
	}

	@Test
	void 대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(master.getId(), new UserApproveCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void 이미_처리된_사용자를_승인하려하면_USER_ALREADY_PROCESSED_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(master.getId(), new UserApproveCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_ALREADY_PROCESSED);
	}

	@Test
	void MASTER는_아무_사용자나_거절할_수_있다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserRejectResult result = userCommandService.reject(master.getId(), new UserRejectCommand(target.getId()));

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.REJECTED);
	}

	@Test
	void COMPANY_MANAGER는_거절_권한이_없다() {
		// given
		User companyManager = createUser("company1", Role.COMPANY_MANAGER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(companyManager.getId())).thenReturn(companyManager);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.reject(companyManager.getId(), new UserRejectCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.REJECT_USER_FORBIDDEN);
	}

	@Test
	void MASTER가_APPROVED_사용자를_정지하면_SUSPENDED_상태가_되고_토큰이_무효화된다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserSuspendResult result = userCommandService.suspend(master.getId(), new UserSuspendCommand(target.getId()));

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
		org.mockito.Mockito.verify(refreshTokenRepository).deleteByUserId(target.getId());
		org.mockito.Mockito.verify(userInvalidationRepository).invalidate(
				org.mockito.ArgumentMatchers.eq(target.getId()), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void MASTER가_아니면_정지_권한이_없다() {
		// given
		User hubManager = createUserWithHub("hub1", Role.HUB_MANAGER, UUID.randomUUID());
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);

		// when & then
		assertThatThrownBy(() -> userCommandService.suspend(hubManager.getId(), new UserSuspendCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.SUSPEND_USER_FORBIDDEN);
	}

	@Test
	void APPROVED가_아닌_사용자를_정지하려하면_USER_NOT_SUSPENDABLE_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.suspend(master.getId(), new UserSuspendCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_SUSPENDABLE);
	}

	@Test
	void 마지막_남은_MASTER를_정지하려하면_LAST_MASTER_SUSPEND_FORBIDDEN_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(1L);

		// when & then
		assertThatThrownBy(() -> userCommandService.suspend(master.getId(), new UserSuspendCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.LAST_MASTER_SUSPEND_FORBIDDEN);
	}

	@Test
	void MASTER가_여러_명이면_그중_하나를_정지할_수_있다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target-master", Role.MASTER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));
		when(userCommandRepository.countActiveMastersForUpdate()).thenReturn(2L);

		// when
		UserSuspendResult result = userCommandService.suspend(master.getId(), new UserSuspendCommand(target.getId()));

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
	}

	@Test
	void 정지_대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userCommandService.suspend(master.getId(), new UserSuspendCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void MASTER가_SUSPENDED_사용자를_정지_해제하면_APPROVED_상태로_돌아간다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		target.approve(UUID.randomUUID());
		target.suspend();
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserReinstateResult result =
				userCommandService.reinstate(master.getId(), new UserReinstateCommand(target.getId()));

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
	}

	@Test
	void MASTER가_아니면_정지_해제_권한이_없다() {
		// given
		User hubManager = createUserWithHub("hub1", Role.HUB_MANAGER, UUID.randomUUID());
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);

		// when & then
		assertThatThrownBy(() -> userCommandService.reinstate(hubManager.getId(), new UserReinstateCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.REINSTATE_USER_FORBIDDEN);
	}

	@Test
	void SUSPENDED가_아닌_사용자를_정지_해제하려하면_USER_NOT_SUSPENDED_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		target.approve(UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.reinstate(master.getId(), new UserReinstateCommand(target.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_SUSPENDED);
	}

	@Test
	void 정지_해제_대상_사용자가_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userCommandRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userCommandService.reinstate(master.getId(), new UserReinstateCommand(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private User createUser(String username, Role role, UUID companyId) {
		User user = User.builder()
				.username(username)
				.password("encoded-password")
				.name("테스트유저")
				.slackId("U" + UUID.randomUUID().toString().substring(0, 10))
				.role(role)
				.companyId(companyId)
				.build();
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}

	private User createUserWithHub(String username, Role role, UUID hubId) {
		User user = User.builder()
				.username(username)
				.password("encoded-password")
				.name("테스트유저")
				.slackId("U" + UUID.randomUUID().toString().substring(0, 10))
				.role(role)
				.hubId(hubId)
				.build();
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}
}
