package com.delivery_project.user_service.user.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

import jakarta.persistence.LockModeType;

public interface SpringDataUserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);

	Page<User> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

	Page<User> findByApprovalStatusAndHubId(ApprovalStatus approvalStatus, UUID hubId, Pageable pageable);

	List<User> findByIdIn(Collection<UUID> ids);

	List<User> findByHubIdAndRoleOrderByCreatedAtAsc(UUID hubId, Role role);

	long countByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);

	/**
	 * "마지막 MASTER 확인 → 정지/삭제" 사이의 레이스를 막기 위해 활성 MASTER 행에 SELECT ... FOR
	 * UPDATE를 건다. 호출하는 쪽이 이미 열려있는 트랜잭션 안에서 불러야 커밋 시점까지 락이
	 * 유지된다(UserCommandRepository.countActiveMastersForUpdate() 참고).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM User u WHERE u.role = :role AND u.approvalStatus = :approvalStatus")
	List<User> findByRoleAndApprovalStatusForUpdate(
			@Param("role") Role role, @Param("approvalStatus") ApprovalStatus approvalStatus);
}
