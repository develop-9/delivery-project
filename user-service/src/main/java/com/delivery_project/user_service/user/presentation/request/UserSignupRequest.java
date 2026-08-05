package com.delivery_project.user_service.user.presentation.request;

import java.util.UUID;

import com.delivery_project.user_service.user.application.command.UserSignupCommand;
import com.delivery_project.user_service.user.domain.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserSignupRequest(
		@NotBlank
		@Pattern(regexp = "^[a-z0-9]{4,10}$", message = "아이디는 4~10자의 소문자와 숫자로만 구성되어야 합니다.")
		String username,

		@NotBlank
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,15}$",
				message = "비밀번호는 8~15자의 영문 대소문자, 숫자, 특수문자를 모두 포함해야 합니다."
		)
		String password,

		@NotBlank
		String name,

		@NotBlank
		String slackId,

		@NotNull
		Role role,

		UUID hubId,

		UUID companyId
) {
	public UserSignupCommand toCommand() {
		return new UserSignupCommand(username, password, name, slackId, role, hubId, companyId);
	}
}
