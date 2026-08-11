package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutboxStatus;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationOutboxRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserInvalidationOutboxRepositoryImpl implements UserInvalidationOutboxRepository {

	private final SpringDataUserInvalidationOutboxRepository springDataUserInvalidationOutboxRepository;

	@Override
	public UserInvalidationOutbox save(UserInvalidationOutbox outbox) {
		return springDataUserInvalidationOutboxRepository.save(outbox);
	}

	@Override
	public Optional<UserInvalidationOutbox> findById(UUID id) {
		return springDataUserInvalidationOutboxRepository.findById(id);
	}

	@Override
	public List<UserInvalidationOutbox> findPendingNotUpdatedSince(Instant threshold) {
		return springDataUserInvalidationOutboxRepository.findByStatusAndUpdatedAtBefore(
				UserInvalidationOutboxStatus.PENDING, threshold);
	}
}
