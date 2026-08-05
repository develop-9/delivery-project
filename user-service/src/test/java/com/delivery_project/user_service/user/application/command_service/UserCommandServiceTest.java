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
import org.springframework.test.util.ReflectionTestUtils;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CallerResolver callerResolver;

	@InjectMocks
	private UserCommandService userCommandService;

	@Test
	void MASTER는_아무_사용자나_승인할_수_있다() {
		// given
		User master = createUser("master1", Role.MASTER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserApproveResult result = userCommandService.approve(master.getId(), target.getId());

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
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserApproveResult result = userCommandService.approve(hubManager.getId(), target.getId());

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
	}

	@Test
	void HUB_MANAGER가_다른_허브_소속_사용자를_승인하려하면_HUB_PERMISSION_DENIED_예외가_발생한다() {
		// given
		User hubManager = createUserWithHub("hub1", Role.HUB_MANAGER, UUID.randomUUID());
		User target = createUserWithHub("target1", Role.DELIVERY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(hubManager.getId(), target.getId()))
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
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(companyManager.getId(), target.getId()))
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
		when(userRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(master.getId(), targetId))
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
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.approve(master.getId(), target.getId()))
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
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserRejectResult result = userCommandService.reject(master.getId(), target.getId());

		// then
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.REJECTED);
	}

	@Test
	void COMPANY_MANAGER는_거절_권한이_없다() {
		// given
		User companyManager = createUser("company1", Role.COMPANY_MANAGER, null);
		User target = createUser("target1", Role.COMPANY_MANAGER, null);
		when(callerResolver.resolve(companyManager.getId())).thenReturn(companyManager);
		when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when & then
		assertThatThrownBy(() -> userCommandService.reject(companyManager.getId(), target.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.REJECT_USER_FORBIDDEN);
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
