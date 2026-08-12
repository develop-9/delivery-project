package com.delivery_project.user_service.user.application.query_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
import com.delivery_project.user_service.user.application.query.InternalUserBatchQuery;
import com.delivery_project.user_service.user.application.query.InternalUserGetQuery;
import com.delivery_project.user_service.user.application.query.InternalUserHubRoleQuery;
import com.delivery_project.user_service.user.application.query.InternalUserSlackQuery;
import com.delivery_project.user_service.user.application.query.UserGetByIdQuery;
import com.delivery_project.user_service.user.application.query.UserListQuery;
import com.delivery_project.user_service.user.application.query.UserPendingQuery;
import com.delivery_project.user_service.user.application.result.InternalUserResult;
import com.delivery_project.user_service.user.application.result.InternalUserSlackResult;
import com.delivery_project.user_service.user.application.result.UserDetailResult;
import com.delivery_project.user_service.user.application.result.UserListResult;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.application.support.pagination.PageValidator;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserQueryRepository;
import com.delivery_project.user_service.user.domain.repository.UserSearchCondition;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

	@Mock
	private UserQueryRepository userQueryRepository;

	@Mock
	private CallerResolver callerResolver;

	@Mock
	private PageValidator pageValidator;

	@InjectMocks
	private UserQueryService userQueryService;

	@Test
	void 본인_정보를_조회하면_호출자가_가진_정보가_그대로_반환된다() {
		// given
		User caller = createUser(Role.COMPANY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(caller.getId())).thenReturn(caller);

		// when
		UserDetailResult result = userQueryService.getMe(caller.getId());

		// then
		assertThat(result.userId()).isEqualTo(caller.getId());
		assertThat(result.username()).isEqualTo(caller.getUsername());
		assertThat(result.slackId()).isEqualTo(caller.getSlackId());
		assertThat(result.role()).isEqualTo(Role.COMPANY_MANAGER);
	}

	@Test
	void MASTER는_조건_없이_전체_사용자_목록을_조회할_수_있다() {
		// given
		User master = createUser(Role.MASTER, null);
		Pageable pageable = PageRequest.of(0, 10);
		UserSearchCondition condition = new UserSearchCondition(null, null, null, null);
		User target = createUser(Role.COMPANY_MANAGER, UUID.randomUUID());
		Page<User> page = new PageImpl<>(List.of(target), pageable, 1);

		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(pageValidator.normalizeSize(pageable)).thenReturn(pageable);
		when(userQueryRepository.search(condition, pageable)).thenReturn(page);

		// when
		Page<UserListResult> result =
				userQueryService.getUsers(master.getId(), new UserListQuery(null, null, null, null, pageable));

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).userId()).isEqualTo(target.getId());
	}

	@Test
	void 목록_조회에서_허용되지_않은_size는_조회_전에_기본값_10으로_보정된다() {
		// given
		User master = createUser(Role.MASTER, null);
		Pageable requestedPageable = PageRequest.of(0, 25);
		Pageable normalizedPageable = PageRequest.of(0, 10);
		UserSearchCondition condition = new UserSearchCondition(null, null, null, null);
		Page<User> page = new PageImpl<>(List.of(), normalizedPageable, 0);

		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(pageValidator.normalizeSize(requestedPageable)).thenReturn(normalizedPageable);
		when(userQueryRepository.search(condition, normalizedPageable)).thenReturn(page);

		// when
		userQueryService.getUsers(master.getId(), new UserListQuery(null, null, null, null, requestedPageable));

		// then
		org.mockito.Mockito.verify(userQueryRepository).search(condition, normalizedPageable);
	}

	@Test
	void MASTER가_아니면_목록_조회_권한이_없다() {
		// given
		User hubManager = createUserWithHub(Role.HUB_MANAGER, UUID.randomUUID());
		Pageable pageable = PageRequest.of(0, 10);
		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);

		// when & then
		assertThatThrownBy(() -> userQueryService.getUsers(
				hubManager.getId(), new UserListQuery(null, null, null, null, pageable)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.READ_USER_FORBIDDEN);
	}

	@Test
	void MASTER는_아무_사용자나_단건_조회할_수_있다() {
		// given
		User master = createUser(Role.MASTER, null);
		User target = createUser(Role.COMPANY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userQueryRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		UserDetailResult result = userQueryService.getById(master.getId(), new UserGetByIdQuery(target.getId()));

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
	}

	@Test
	void 본인이_아닌_사용자를_MASTER가_아닌_역할이_조회하면_대상_존재_여부와_무관하게_조회_전에_READ_USER_FORBIDDEN_예외가_발생한다() {
		// given
		User companyManager = createUser(Role.COMPANY_MANAGER, UUID.randomUUID());
		UUID otherId = UUID.randomUUID();
		when(callerResolver.resolve(companyManager.getId())).thenReturn(companyManager);

		// when & then
		assertThatThrownBy(() -> userQueryService.getById(companyManager.getId(), new UserGetByIdQuery(otherId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.READ_USER_FORBIDDEN);
		org.mockito.Mockito.verify(userQueryRepository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 본인_ID로_단건_조회하면_MASTER가_아니어도_조회된다() {
		// given
		User companyManager = createUser(Role.COMPANY_MANAGER, UUID.randomUUID());
		when(callerResolver.resolve(companyManager.getId())).thenReturn(companyManager);
		when(userQueryRepository.findById(companyManager.getId())).thenReturn(Optional.of(companyManager));

		// when
		UserDetailResult result = userQueryService.getById(companyManager.getId(), new UserGetByIdQuery(companyManager.getId()));

		// then
		assertThat(result.userId()).isEqualTo(companyManager.getId());
	}

	@Test
	void 존재하지_않는_사용자를_단건_조회하면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		User master = createUser(Role.MASTER, null);
		UUID targetId = UUID.randomUUID();
		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(userQueryRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userQueryService.getById(master.getId(), new UserGetByIdQuery(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void MASTER가_hubId_없이_조회하면_전체_승인대기자를_조회한다() {
		// given
		User master = createUser(Role.MASTER, null);
		Pageable pageable = PageRequest.of(0, 10);
		User pending = createUser(Role.COMPANY_MANAGER, null);
		Page<User> page = new PageImpl<>(List.of(pending), pageable, 1);

		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(pageValidator.normalizeSize(pageable)).thenReturn(pageable);
		when(userQueryRepository.findAllPending(pageable)).thenReturn(page);

		// when
		Page<UserPendingResult> result = userQueryService.getPendingUsers(master.getId(), new UserPendingQuery(null, pageable));

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

		when(callerResolver.resolve(master.getId())).thenReturn(master);
		when(pageValidator.normalizeSize(pageable)).thenReturn(pageable);
		when(userQueryRepository.findPendingByHub(hubId, pageable)).thenReturn(page);

		// when
		Page<UserPendingResult> result = userQueryService.getPendingUsers(master.getId(), new UserPendingQuery(hubId, pageable));

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

		when(callerResolver.resolve(hubManager.getId())).thenReturn(hubManager);
		when(pageValidator.normalizeSize(pageable)).thenReturn(pageable);
		when(userQueryRepository.findPendingByHub(myHubId, pageable)).thenReturn(page);

		// when
		userQueryService.getPendingUsers(hubManager.getId(), new UserPendingQuery(requestedHubId, pageable));

		// then
		org.mockito.Mockito.verify(userQueryRepository).findPendingByHub(myHubId, pageable);
	}

	@Test
	void COMPANY_MANAGER는_승인대기자_조회_권한이_없다() {
		// given
		User companyManager = createUser(Role.COMPANY_MANAGER, null);
		Pageable pageable = PageRequest.of(0, 10);
		when(callerResolver.resolve(companyManager.getId())).thenReturn(companyManager);
		when(pageValidator.normalizeSize(pageable)).thenReturn(pageable);

		// when & then
		assertThatThrownBy(() -> userQueryService.getPendingUsers(companyManager.getId(), new UserPendingQuery(null, pageable)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.READ_USER_FORBIDDEN);
	}

	@Test
	void DELIVERY_MANAGER는_승인대기자_조회_권한이_없다() {
		// given
		User deliveryManager = createUser(Role.DELIVERY_MANAGER, null);
		Pageable pageable = PageRequest.of(0, 10);
		when(callerResolver.resolve(deliveryManager.getId())).thenReturn(deliveryManager);
		when(pageValidator.normalizeSize(pageable)).thenReturn(pageable);

		// when & then
		assertThatThrownBy(() -> userQueryService.getPendingUsers(deliveryManager.getId(), new UserPendingQuery(null, pageable)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.READ_USER_FORBIDDEN);
	}

	@Test
	void Internal_단건_조회는_인증_없이_사용자_정보를_반환한다() {
		// given
		User target = createUser(Role.HUB_MANAGER, null);
		when(userQueryRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		InternalUserResult result = userQueryService.getInternalUser(new InternalUserGetQuery(target.getId()));

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(result.username()).isEqualTo(target.getUsername());
	}

	@Test
	void Internal_배치_조회는_존재하는_id만_결과에_포함한다() {
		// given
		User target1 = createUser(Role.COMPANY_MANAGER, null);
		User target2 = createUser(Role.HUB_MANAGER, null);
		UUID notFoundId = UUID.randomUUID();
		List<UUID> ids = List.of(target1.getId(), target2.getId(), notFoundId);
		when(userQueryRepository.findAllByIds(ids)).thenReturn(List.of(target1, target2));

		// when
		List<InternalUserResult> results = userQueryService.getInternalUsersBatch(new InternalUserBatchQuery(ids));

		// then
		assertThat(results).hasSize(2);
		assertThat(results).extracting(InternalUserResult::userId)
				.containsExactlyInAnyOrder(target1.getId(), target2.getId());
	}

	@Test
	void Internal_배치_조회는_최대_100건까지만_허용하고_초과하면_예외가_발생한다() {
		// given
		List<UUID> tooManyIds = java.util.stream.Stream.generate(UUID::randomUUID)
				.limit(101)
				.toList();

		// when & then
		assertThatThrownBy(() -> userQueryService.getInternalUsersBatch(new InternalUserBatchQuery(tooManyIds)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_BATCH_SIZE);
		org.mockito.Mockito.verify(userQueryRepository, org.mockito.Mockito.never()).findAllByIds(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void Internal_배치_조회는_정확히_100건이면_허용된다() {
		// given
		List<UUID> exactlyMaxIds = java.util.stream.Stream.generate(UUID::randomUUID)
				.limit(100)
				.toList();
		when(userQueryRepository.findAllByIds(exactlyMaxIds)).thenReturn(List.of());

		// when
		userQueryService.getInternalUsersBatch(new InternalUserBatchQuery(exactlyMaxIds));

		// then
		org.mockito.Mockito.verify(userQueryRepository).findAllByIds(exactlyMaxIds);
	}

	@Test
	void Internal_허브_역할_조회는_조건에_맞는_사용자를_반환한다() {
		// given
		UUID hubId = UUID.randomUUID();
		User hubManager = createUserWithHub(Role.HUB_MANAGER, hubId);
		when(userQueryRepository.findByHubIdAndRole(hubId, Role.HUB_MANAGER)).thenReturn(List.of(hubManager));

		// when
		InternalUserResult result = userQueryService.getInternalUserByHubAndRole(new InternalUserHubRoleQuery(hubId, Role.HUB_MANAGER));

		// then
		assertThat(result.userId()).isEqualTo(hubManager.getId());
	}

	@Test
	void Internal_허브_역할_조회에_매칭되는_사용자가_없으면_INTERNAL_USER_NOT_FOUND_예외가_발생한다() {
		// given
		UUID hubId = UUID.randomUUID();
		when(userQueryRepository.findByHubIdAndRole(hubId, Role.HUB_MANAGER)).thenReturn(List.of());

		// when & then
		assertThatThrownBy(() -> userQueryService.getInternalUserByHubAndRole(new InternalUserHubRoleQuery(hubId, Role.HUB_MANAGER)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INTERNAL_USER_NOT_FOUND);
	}

	@Test
	void Internal_허브_역할_조회에_매칭되는_사용자가_없으면_요청한_역할이_에러_메시지에_포함된다() {
		// given
		UUID hubId = UUID.randomUUID();
		when(userQueryRepository.findByHubIdAndRole(hubId, Role.DELIVERY_MANAGER)).thenReturn(List.of());

		// when & then
		assertThatThrownBy(() -> userQueryService.getInternalUserByHubAndRole(new InternalUserHubRoleQuery(hubId, Role.DELIVERY_MANAGER)))
				.isInstanceOf(BusinessException.class)
				.extracting(Throwable::getMessage)
				.asString()
				.contains("DELIVERY_MANAGER");
	}

	@Test
	void Internal_허브_역할_조회에_여러_건이_매칭돼도_예외_없이_첫_번째를_반환한다() {
		// given
		UUID hubId = UUID.randomUUID();
		User first = createUserWithHub(Role.DELIVERY_MANAGER, hubId);
		User second = createUserWithHub(Role.DELIVERY_MANAGER, hubId);
		when(userQueryRepository.findByHubIdAndRole(hubId, Role.DELIVERY_MANAGER))
				.thenReturn(List.of(first, second));

		// when
		InternalUserResult result = userQueryService.getInternalUserByHubAndRole(new InternalUserHubRoleQuery(hubId, Role.DELIVERY_MANAGER));

		// then
		assertThat(result.userId()).isEqualTo(first.getId());
	}

	@Test
	void Internal_Slack_ID_조회는_사용자의_Slack_ID를_반환한다() {
		// given
		User target = createUser(Role.DELIVERY_MANAGER, null);
		when(userQueryRepository.findById(target.getId())).thenReturn(Optional.of(target));

		// when
		InternalUserSlackResult result = userQueryService.getInternalUserSlack(new InternalUserSlackQuery(target.getId()));

		// then
		assertThat(result.userId()).isEqualTo(target.getId());
		assertThat(result.slackId()).isEqualTo(target.getSlackId());
	}

	@Test
	void Internal_Slack_ID_조회_대상이_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		UUID targetId = UUID.randomUUID();
		when(userQueryRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userQueryService.getInternalUserSlack(new InternalUserSlackQuery(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void Internal_단건_조회_대상이_없으면_USER_NOT_FOUND_예외가_발생한다() {
		// given
		UUID targetId = UUID.randomUUID();
		when(userQueryRepository.findById(targetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userQueryService.getInternalUser(new InternalUserGetQuery(targetId)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private User createUser(Role role, UUID companyId) {
		// hubId/companyId는 도메인 불변식(User 생성자)이라, 역할 검증 자체가 관심사가 아닌
		// 호출부가 null을 넘겨도 깨지지 않도록 역할에 맞는 값을 여기서 채워준다.
		UUID resolvedCompanyId = (role == Role.COMPANY_MANAGER && companyId == null)
				? UUID.randomUUID()
				: companyId;
		UUID resolvedHubId = (role == Role.HUB_MANAGER || role == Role.DELIVERY_MANAGER)
				? UUID.randomUUID()
				: null;

		User user = User.builder()
				.username("user1")
				.password("encoded-password")
				.name("테스트유저")
				.slackId("U" + UUID.randomUUID().toString().substring(0, 10))
				.role(role)
				.companyId(resolvedCompanyId)
				.hubId(resolvedHubId)
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
