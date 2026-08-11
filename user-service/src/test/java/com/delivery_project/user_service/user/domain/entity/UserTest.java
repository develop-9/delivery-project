package com.delivery_project.user_service.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

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
	void 이미_처리된_사용자를_다시_승인하려하면_IllegalStateException이_발생한다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());

		// when & then
		assertThatThrownBy(() -> user.approve(UUID.randomUUID()))
				.isInstanceOf(IllegalStateException.class);
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
	void 이미_처리된_사용자를_다시_거절하려하면_IllegalStateException이_발생한다() {
		// given
		User user = createUser();
		user.reject();

		// when & then
		assertThatThrownBy(user::reject)
				.isInstanceOf(IllegalStateException.class);
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
