package com.delivery_project.user_service.user.presentation.api_controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.user_service.global.response.PageResponse;
import com.delivery_project.user_service.global.response.SuccessResponse;
import com.delivery_project.user_service.user.application.command_service.UserCommandService;
import com.delivery_project.user_service.user.application.query_service.UserQueryService;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserDetailResult;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.application.result.UserUpdateMeResult;
import com.delivery_project.user_service.user.presentation.request.UserUpdateMeRequest;
import com.delivery_project.user_service.user.presentation.response.UserApproveResponse;
import com.delivery_project.user_service.user.presentation.response.UserDetailResponse;
import com.delivery_project.user_service.user.presentation.response.UserPendingResponse;
import com.delivery_project.user_service.user.presentation.response.UserRejectResponse;
import com.delivery_project.user_service.user.presentation.response.UserUpdateMeResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

	private final UserCommandService userCommandService;
	private final UserQueryService userQueryService;

	@GetMapping("/me")
	public ResponseEntity<SuccessResponse<UserDetailResponse>> getMe(@AuthenticationPrincipal UUID callerId) {
		UserDetailResult result = userQueryService.getMe(callerId);
		return ResponseEntity.ok(SuccessResponse.success(UserDetailResponse.from(result)));
	}

	@PatchMapping("/me")
	public ResponseEntity<SuccessResponse<UserUpdateMeResponse>> updateMe(
			@AuthenticationPrincipal UUID callerId,
			@Valid @RequestBody UserUpdateMeRequest request
	) {
		UserUpdateMeResult result = userCommandService.updateMe(callerId, request.toCommand());
		return ResponseEntity.ok(SuccessResponse.success(UserUpdateMeResponse.from(result)));
	}

	@GetMapping("/pending")
	public ResponseEntity<SuccessResponse<PageResponse<UserPendingResponse>>> getPendingUsers(
			@AuthenticationPrincipal UUID callerId,
			@RequestParam(required = false) UUID hubId,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<UserPendingResult> results = userQueryService.getPendingUsers(callerId, hubId, pageable);
		PageResponse<UserPendingResponse> response = PageResponse.from(results, UserPendingResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@PatchMapping("/{userId}/approve")
	public ResponseEntity<SuccessResponse<UserApproveResponse>> approve(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID userId
	) {
		UserApproveResult result = userCommandService.approve(callerId, userId);
		return ResponseEntity.ok(SuccessResponse.success(UserApproveResponse.from(result)));
	}

	@PatchMapping("/{userId}/reject")
	public ResponseEntity<SuccessResponse<UserRejectResponse>> reject(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID userId
	) {
		UserRejectResult result = userCommandService.reject(callerId, userId);
		return ResponseEntity.ok(SuccessResponse.success(UserRejectResponse.from(result)));
	}
}
