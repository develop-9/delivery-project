package com.delivery_project.user_service.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;

class UserTest {

	@Test
	void PENDING_상태의_사용자를_승인하면_APPROVED_상태가_되고_승인정보가_기록된다() {
		// given
		User user = createUser();
		UUID approverId = UUID.randomUUID();

		// when
		user.approve(approverId);

		// then
		assertThat(user.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
		assertThat(user.getApprovedBy()).isEqualTo(approverId);
		assertThat(user.getApprovedAt()).isNotNull();
	}

	@Test
	void 이미_처리된_사용자를_다시_승인하려하면_USER_ALREADY_PROCESSED_예외가_발생한다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());

		// when & then
		assertThatThrownBy(() -> user.approve(UUID.randomUUID()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_ALREADY_PROCESSED);
	}

	@Test
	void PENDING_상태의_사용자를_초기_MASTER로_자동승인하면_APPROVED_상태가_되고_승인자는_비어있다() {
		// given
		User user = createUser();

		// when
		user.approveAsInitialMaster();

		// then
		assertThat(user.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
		assertThat(user.getApprovedBy()).isNull();
		assertThat(user.getApprovedAt()).isNotNull();
	}

	@Test
	void 이미_처리된_사용자를_초기_MASTER로_다시_자동승인하려하면_IllegalStateException이_발생한다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());

		// when & then
		assertThatThrownBy(user::approveAsInitialMaster)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void PENDING_상태의_사용자를_거절하면_REJECTED_상태가_된다() {
		// given
		User user = createUser();

		// when
		user.reject();

		// then
		assertThat(user.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
	}

	@Test
	void 이미_처리된_사용자를_다시_거절하려하면_USER_ALREADY_PROCESSED_예외가_발생한다() {
		// given
		User user = createUser();
		user.reject();

		// when & then
		assertThatThrownBy(user::reject)
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_ALREADY_PROCESSED);
	}

	@Test
	void APPROVED_상태의_사용자를_정지하면_SUSPENDED_상태가_된다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());

		// when
		user.suspend();

		// then
		assertThat(user.getApprovalStatus()).isEqualTo(ApprovalStatus.SUSPENDED);
	}

	@Test
	void APPROVED가_아닌_사용자를_정지하려하면_IllegalStateException이_발생한다() {
		// given
		User user = createUser();

		// when & then
		assertThatThrownBy(user::suspend)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void SUSPENDED_상태의_사용자를_정지_해제하면_APPROVED_상태로_돌아간다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());
		user.suspend();

		// when
		user.reinstate();

		// then
		assertThat(user.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
	}

	@Test
	void SUSPENDED가_아닌_사용자를_정지_해제하려하면_IllegalStateException이_발생한다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());

		// when & then
		assertThatThrownBy(user::reinstate)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void HUB_MANAGER인데_hubId가_없으면_생성_시점에_BusinessException이_발생한다() {
		// when & then
		assertThatThrownBy(() -> User.builder()
				.username("hub1")
				.password("encoded-password")
				.name("허브담당자")
				.slackId("U0000000001")
				.role(Role.HUB_MANAGER)
				.build())
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void DELIVERY_MANAGER인데_hubId가_없으면_생성_시점에_BusinessException이_발생한다() {
		// when & then
		assertThatThrownBy(() -> User.builder()
				.username("delivery1")
				.password("encoded-password")
				.name("배송담당자")
				.slackId("U0000000002")
				.role(Role.DELIVERY_MANAGER)
				.build())
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void COMPANY_MANAGER인데_companyId가_없으면_생성_시점에_BusinessException이_발생한다() {
		// when & then
		assertThatThrownBy(() -> User.builder()
				.username("company1")
				.password("encoded-password")
				.name("업체담당자")
				.slackId("U0000000003")
				.role(Role.COMPANY_MANAGER)
				.build())
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void MASTER는_hubId_companyId_없이도_생성된다() {
		// when
		User user = User.builder()
				.username("master1")
				.password("encoded-password")
				.name("관리자")
				.slackId("U0000000004")
				.role(Role.MASTER)
				.build();

		// then
		assertThat(user.getHubId()).isNull();
		assertThat(user.getCompanyId()).isNull();
	}

	private User createUser() {
		return User.builder()
				.username("kim123")
				.password("encoded-password")
				.name("김철수")
				.slackId("U0123456789")
				.role(Role.COMPANY_MANAGER)
				.companyId(UUID.randomUUID())
				.build();
	}
}
