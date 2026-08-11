package com.delivery_project.user_service.user.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutbox;
import com.delivery_project.user_service.user.domain.entity.UserInvalidationOutboxStatus;

public interface SpringDataUserInvalidationOutboxRepository extends JpaRepository<UserInvalidationOutbox, UUID> {

	List<UserInvalidationOutbox> findByStatusAndUpdatedAtBefore(UserInvalidationOutboxStatus status, Instant threshold);
}
