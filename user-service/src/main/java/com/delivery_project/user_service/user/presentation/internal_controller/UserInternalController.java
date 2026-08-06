package com.delivery_project.user_service.user.presentation.internal_controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.user_service.global.response.SuccessResponse;
import com.delivery_project.user_service.user.application.query.InternalUserBatchQuery;
import com.delivery_project.user_service.user.application.query.InternalUserGetQuery;
import com.delivery_project.user_service.user.application.query.InternalUserHubRoleQuery;
import com.delivery_project.user_service.user.application.query.InternalUserSlackQuery;
import com.delivery_project.user_service.user.application.query_service.UserQueryService;
import com.delivery_project.user_service.user.application.result.InternalUserResult;
import com.delivery_project.user_service.user.application.result.InternalUserSlackResult;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.presentation.response.InternalUserResponse;
import com.delivery_project.user_service.user.presentation.response.InternalUserSlackResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class UserInternalController {

	private final UserQueryService userQueryService;

	@GetMapping("/{userId}")
	public ResponseEntity<SuccessResponse<InternalUserResponse>> getUser(@PathVariable UUID userId) {
		InternalUserResult result = userQueryService.getInternalUser(new InternalUserGetQuery(userId));
		return ResponseEntity.ok(SuccessResponse.success(InternalUserResponse.from(result)));
	}

	@GetMapping("/batch")
	public ResponseEntity<SuccessResponse<List<InternalUserResponse>>> getUsersBatch(@RequestParam List<UUID> ids) {
		List<InternalUserResponse> responses = userQueryService.getInternalUsersBatch(new InternalUserBatchQuery(ids))
				.stream()
				.map(InternalUserResponse::from)
				.toList();
		return ResponseEntity.ok(SuccessResponse.success(responses));
	}

	@GetMapping
	public ResponseEntity<SuccessResponse<InternalUserResponse>> getUserByHubAndRole(
			@RequestParam UUID hubId,
			@RequestParam Role role
	) {
		InternalUserResult result = userQueryService.getInternalUserByHubAndRole(
				new InternalUserHubRoleQuery(hubId, role));
		return ResponseEntity.ok(SuccessResponse.success(InternalUserResponse.from(result)));
	}

	@GetMapping("/{userId}/slack")
	public ResponseEntity<SuccessResponse<InternalUserSlackResponse>> getUserSlack(@PathVariable UUID userId) {
		InternalUserSlackResult result = userQueryService.getInternalUserSlack(new InternalUserSlackQuery(userId));
		return ResponseEntity.ok(SuccessResponse.success(InternalUserSlackResponse.from(result)));
	}
}
