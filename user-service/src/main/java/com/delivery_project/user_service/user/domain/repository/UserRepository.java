package com.delivery_project.user_service.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.entity.User;

public interface UserRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);

	Page<User> findAllPending(Pageable pageable);

	Page<User> findPendingByHub(UUID hubId, Pageable pageable);
}
