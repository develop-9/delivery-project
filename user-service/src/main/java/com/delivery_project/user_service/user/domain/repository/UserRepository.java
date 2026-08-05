package com.delivery_project.user_service.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;

public interface UserRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);

	Page<User> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

	Page<User> findByApprovalStatusAndHubId(ApprovalStatus approvalStatus, UUID hubId, Pageable pageable);
}
