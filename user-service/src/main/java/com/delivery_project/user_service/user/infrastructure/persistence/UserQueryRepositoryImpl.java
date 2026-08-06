package com.delivery_project.user_service.user.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

	private final SpringDataUserRepository springDataUserRepository;

	@Override
	public Page<User> findAllPending(Pageable pageable) {
		return springDataUserRepository.findByApprovalStatus(ApprovalStatus.PENDING, pageable);
	}

	@Override
	public Page<User> findPendingByHub(UUID hubId, Pageable pageable) {
		return springDataUserRepository.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, hubId, pageable);
	}
}
