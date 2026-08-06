package com.delivery_project.user_service.user.application.result;

import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

/** Internal API(단건/배치/허브-역할 조회)가 공유하는 결과. */
public record InternalUserResult(
		UUID userId,
		String username,
		String name,
		Role role,
		UUID hubId,
		UUID companyId
) {
	public static InternalUserResult from(User user) {
		return new InternalUserResult(
				user.getId(), user.getUsername(), user.getName(), user.getRole(), user.getHubId(), user.getCompanyId());
	}
}
