package com.delivery_project.user_service.user.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

public interface SpringDataUserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);

	Page<User> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

	Page<User> findByApprovalStatusAndHubId(ApprovalStatus approvalStatus, UUID hubId, Pageable pageable);

	List<User> findByIdIn(Collection<UUID> ids);

	List<User> findByHubIdAndRole(UUID hubId, Role role);

	long countByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);
}
