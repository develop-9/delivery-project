package com.delivery_project.user_service.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery_project.user_service.user.domain.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);
}
