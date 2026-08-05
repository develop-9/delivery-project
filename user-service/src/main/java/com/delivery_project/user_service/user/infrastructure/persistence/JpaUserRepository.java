package com.delivery_project.user_service.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

	private final SpringDataUserRepository springDataUserRepository;

	@Override
	public User save(User user) {
		return springDataUserRepository.save(user);
	}

	@Override
	public Optional<User> findById(UUID id) {
		return springDataUserRepository.findById(id);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return springDataUserRepository.findByUsername(username);
	}

	@Override
	public boolean existsByUsername(String username) {
		return springDataUserRepository.existsByUsername(username);
	}

	@Override
	public boolean existsBySlackId(String slackId) {
		return springDataUserRepository.existsBySlackId(slackId);
	}

	@Override
	public Page<User> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable) {
		return springDataUserRepository.findByApprovalStatus(approvalStatus, pageable);
	}

	@Override
	public Page<User> findByApprovalStatusAndHubId(ApprovalStatus approvalStatus, UUID hubId, Pageable pageable) {
		return springDataUserRepository.findByApprovalStatusAndHubId(approvalStatus, hubId, pageable);
	}
}
