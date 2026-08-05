package com.delivery_project.user_service.user.infrastructure.persistence;

import static com.delivery_project.user_service.user.domain.entity.QUser.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserQueryRepository;
import com.delivery_project.user_service.user.domain.repository.UserSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 목록 조회(search)는 조건이 전부 선택적이라 파생 쿼리로는 표현이 안 돼 QueryDSL을 쓴다.
 *
 * deleted_at IS NULL은 User의 @SQLRestriction이 붙여준다.
 * QueryDSL이 만든 JPQL에도 적용되므로 여기서 조건을 쓰지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

	private final SpringDataUserRepository springDataUserRepository;
	private final JPAQueryFactory queryFactory;

	@Override
	public Page<User> findAllPending(Pageable pageable) {
		return springDataUserRepository.findByApprovalStatus(ApprovalStatus.PENDING, pageable);
	}

	@Override
	public Page<User> findPendingByHub(UUID hubId, Pageable pageable) {
		return springDataUserRepository.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, hubId, pageable);
	}

	@Override
	public Optional<User> findById(UUID id) {
		return springDataUserRepository.findById(id);
	}

	@Override
	public List<User> findAllByIds(Collection<UUID> ids) {
		return springDataUserRepository.findByIdIn(ids);
	}

	@Override
	public Optional<User> findByHubIdAndRole(UUID hubId, Role role) {
		return springDataUserRepository.findByHubIdAndRole(hubId, role);
	}

	@Override
	public Page<User> search(UserSearchCondition condition, Pageable pageable) {
		List<User> content = queryFactory
				.selectFrom(user)
				.where(
						approvalStatusEq(condition.approvalStatus()),
						roleEq(condition.role()),
						hubIdEq(condition.hubId()),
						companyIdEq(condition.companyId())
				)
				.orderBy(AuditSortSupport.toOrderSpecifiers(pageable.getSort(), user.createdAt, user.updatedAt))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		JPAQuery<Long> countQuery = queryFactory
				.select(user.count())
				.from(user)
				.where(
						approvalStatusEq(condition.approvalStatus()),
						roleEq(condition.role()),
						hubIdEq(condition.hubId()),
						companyIdEq(condition.companyId())
				);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	private BooleanExpression approvalStatusEq(ApprovalStatus approvalStatus) {
		return approvalStatus == null ? null : user.approvalStatus.eq(approvalStatus);
	}

	private BooleanExpression roleEq(Role role) {
		return role == null ? null : user.role.eq(role);
	}

	private BooleanExpression hubIdEq(UUID hubId) {
		return hubId == null ? null : user.hubId.eq(hubId);
	}

	private BooleanExpression companyIdEq(UUID companyId) {
		return companyId == null ? null : user.companyId.eq(companyId);
	}
}
