package com.delivery_project.user_service.user.presentation.api_controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.user_service.global.response.PageResponse;
import com.delivery_project.user_service.global.response.SuccessResponse;
import com.delivery_project.user_service.user.application.command.UserApproveCommand;
import com.delivery_project.user_service.user.application.command.UserDeleteCommand;
import com.delivery_project.user_service.user.application.command.UserRejectCommand;
import com.delivery_project.user_service.user.application.command_service.UserCommandService;
import com.delivery_project.user_service.user.application.query.UserGetByIdQuery;
import com.delivery_project.user_service.user.application.query.UserListQuery;
import com.delivery_project.user_service.user.application.query.UserPendingQuery;
import com.delivery_project.user_service.user.application.query_service.UserQueryService;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserDeleteResult;
import com.delivery_project.user_service.user.application.result.UserDetailResult;
import com.delivery_project.user_service.user.application.result.UserListResult;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.application.result.UserUpdateMeResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.repository.UserSearchCondition;
import com.delivery_project.user_service.user.presentation.request.UserUpdateMeRequest;
import com.delivery_project.user_service.user.presentation.response.UserApproveResponse;
import com.delivery_project.user_service.user.presentation.response.UserDeleteResponse;
import com.delivery_project.user_service.user.presentation.response.UserDetailResponse;
import com.delivery_project.user_service.user.presentation.response.UserListResponse;
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

	@GetMapping
	public ResponseEntity<SuccessResponse<PageResponse<UserListResponse>>> getUsers(
			@AuthenticationPrincipal UUID callerId,
			@RequestParam(required = false) ApprovalStatus approvalStatus,
			@RequestParam(required = false) Role role,
			@RequestParam(required = false) UUID hubId,
			@RequestParam(required = false) UUID companyId,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		UserSearchCondition condition = new UserSearchCondition(approvalStatus, role, hubId, companyId);
		Page<UserListResult> results = userQueryService.getUsers(callerId, new UserListQuery(condition, pageable));
		PageResponse<UserListResponse> response = PageResponse.from(results, UserListResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@GetMapping("/pending")
	public ResponseEntity<SuccessResponse<PageResponse<UserPendingResponse>>> getPendingUsers(
			@AuthenticationPrincipal UUID callerId,
			@RequestParam(required = false) UUID hubId,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<UserPendingResult> results =
				userQueryService.getPendingUsers(callerId, new UserPendingQuery(hubId, pageable));
		PageResponse<UserPendingResponse> response = PageResponse.from(results, UserPendingResponse::from);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@GetMapping("/{userId}")
	public ResponseEntity<SuccessResponse<UserDetailResponse>> getById(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID userId
	) {
		UserDetailResult result = userQueryService.getById(callerId, new UserGetByIdQuery(userId));
		return ResponseEntity.ok(SuccessResponse.success(UserDetailResponse.from(result)));
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<SuccessResponse<UserDeleteResponse>> delete(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID userId
	) {
		UserDeleteResult result = userCommandService.delete(callerId, new UserDeleteCommand(userId));
		return ResponseEntity.ok(SuccessResponse.success(UserDeleteResponse.from(result)));
	}

	@PatchMapping("/{userId}/approve")
	public ResponseEntity<SuccessResponse<UserApproveResponse>> approve(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID userId
	) {
		UserApproveResult result = userCommandService.approve(callerId, new UserApproveCommand(userId));
		return ResponseEntity.ok(SuccessResponse.success(UserApproveResponse.from(result)));
	}

	@PatchMapping("/{userId}/reject")
	public ResponseEntity<SuccessResponse<UserRejectResponse>> reject(
			@AuthenticationPrincipal UUID callerId,
			@PathVariable UUID userId
	) {
		UserRejectResult result = userCommandService.reject(callerId, new UserRejectCommand(userId));
		return ResponseEntity.ok(SuccessResponse.success(UserRejectResponse.from(result)));
	}
}
