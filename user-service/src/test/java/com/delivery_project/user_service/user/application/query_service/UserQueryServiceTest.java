package com.delivery_project.user_service.user.application.query_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;
import com.delivery_project.user_service.user.infrastructure.jwt.CurrentUserResolver;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrentUserResolver currentUserResolver;

	@InjectMocks
	private UserQueryService userQueryService;

	@Test
	void MASTER가_hubId_없이_조회하면_전체_승인대기자를_조회한다() {
		// given
		User master = createUser(Role.MASTER, null);
		Pageable pageable = PageRequest.of(0, 10);
		User pending = createUser(Role.COMPANY_MANAGER, null);
		Page<User> page = new PageImpl<>(List.of(pending), pageable, 1);

		when(currentUserResolver.resolve("Bearer token")).thenReturn(master);
		when(userRepository.findByApprovalStatus(ApprovalStatus.PENDING, pageable)).thenReturn(page);

		// when
		Page<UserPendingResult> result = userQueryService.getPendingUsers("Bearer token", null, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).userId()).isEqualTo(pending.getId());
	}

	@Test
	void MASTER가_hubId를_지정하면_해당_허브만_조회한다() {
		// given
		User master = createUser(Role.MASTER, null);
		UUID hubId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> page = new PageImpl<>(List.of(), pageable, 0);

		when(currentUserResolver.resolve("Bearer token")).thenReturn(master);
		when(userRepository.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, hubId, pageable)).thenReturn(page);

		// when
		Page<UserPendingResult> result = userQueryService.getPendingUsers("Bearer token", hubId, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(0);
	}

	@Test
	void HUB_MANAGER는_hubId_파라미터와_무관하게_본인_담당_허브로_강제_필터된다() {
		// given
		UUID myHubId = UUID.randomUUID();
		User hubManager = createUserWithHub(Role.HUB_MANAGER, myHubId);
		UUID requestedHubId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> page = new PageImpl<>(List.of(), pageable, 0);

		when(currentUserResolver.resolve("Bearer token")).thenReturn(hubManager);
		when(userRepository.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, myHubId, pageable)).thenReturn(page);

		// when
		userQueryService.getPendingUsers("Bearer token", requestedHubId, pageable);

		// then
		org.mockito.Mockito.verify(userRepository)
				.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, myHubId, pageable);
	}

	@Test
	void COMPANY_MANAGER는_승인대기자_조회_권한이_없다() {
		// given
		User companyManager = createUser(Role.COMPANY_MANAGER, null);
		Pageable pageable = PageRequest.of(0, 10);
		when(currentUserResolver.resolve("Bearer token")).thenReturn(companyManager);

		// when & then
		assertThatThrownBy(() -> userQueryService.getPendingUsers("Bearer token", null, pageable))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.READ_USER_FORBIDDEN);
	}

	@Test
	void DELIVERY_MANAGER는_승인대기자_조회_권한이_없다() {
		// given
		User deliveryManager = createUser(Role.DELIVERY_MANAGER, null);
		Pageable pageable = PageRequest.of(0, 10);
		when(currentUserResolver.resolve("Bearer token")).thenReturn(deliveryManager);

		// when & then
		assertThatThrownBy(() -> userQueryService.getPendingUsers("Bearer token", null, pageable))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.READ_USER_FORBIDDEN);
	}

	private User createUser(Role role, UUID companyId) {
		User user = User.builder()
				.username("user1")
				.password("encoded-password")
				.name("테스트유저")
				.slackId("U" + UUID.randomUUID().toString().substring(0, 10))
				.role(role)
				.companyId(companyId)
				.build();
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}

	private User createUserWithHub(Role role, UUID hubId) {
		User user = User.builder()
				.username("user1")
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
