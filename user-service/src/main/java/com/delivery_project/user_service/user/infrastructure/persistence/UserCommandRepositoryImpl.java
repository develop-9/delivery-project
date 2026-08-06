package com.delivery_project.user_service.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserCommandRepositoryImpl implements UserCommandRepository {

	private final SpringDataUserRepository springDataUserRepository;

	@Override
	public User save(User user) {
		// saveAndFlush로 즉시 DB에 반영해야 username/slackId UNIQUE 제약 위반이 이 시점에 바로
		// 터진다. save()만 쓰면 Hibernate가 flush를 트랜잭션 커밋 시점까지 미뤄서, 서비스
		// 계층의 DataIntegrityViolationException catch가 무력화되고 커밋 시점에야 실패한다.
		return springDataUserRepository.saveAndFlush(user);
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
}
