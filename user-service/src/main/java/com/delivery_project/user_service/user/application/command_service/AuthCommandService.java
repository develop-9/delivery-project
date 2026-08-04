package com.delivery_project.user_service.user.application.command_service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.command.UserSignupCommand;
import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserSignupResult signup(UserSignupCommand command) {
		log.info("[Auth] 회원가입 시도 username={}", command.username());

		validateHubOrCompanyRequired(command);

		if (userRepository.existsByUsername(command.username())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_USERNAME);
		}
		if (userRepository.existsBySlackId(command.slackId())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
		}

		// TODO: hubId/companyId 존재 검증 (Hub/Company Internal API 연동 후 추가, 현재는 입력값 그대로 저장)

		User user = User.builder()
				.username(command.username())
				.password(passwordEncoder.encode(command.password()))
				.name(command.name())
				.slackId(command.slackId())
				.role(command.role())
				.hubId(command.hubId())
				.companyId(command.companyId())
				.build();

		User saved = userRepository.save(user);
		log.info("[Auth] 회원가입 완료 userId={}", saved.getId());

		return UserSignupResult.from(saved);
	}

	private void validateHubOrCompanyRequired(UserSignupCommand command) {
		Role role = command.role();

		if ((role == Role.HUB_MANAGER || role == Role.DELIVERY_MANAGER) && command.hubId() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (role == Role.COMPANY_MANAGER && command.companyId() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}
}
