package com.delivery_project.user_service.user.presentation.api_controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.delivery_project.user_service.global.response.PageResponse;
import com.delivery_project.user_service.global.response.SuccessResponse;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.presentation.request.UserUpdateMeRequest;
import com.delivery_project.user_service.user.presentation.response.UserApproveResponse;
import com.delivery_project.user_service.user.presentation.response.UserDeleteResponse;
import com.delivery_project.user_service.user.presentation.response.UserDetailResponse;
import com.delivery_project.user_service.user.presentation.response.UserListResponse;
import com.delivery_project.user_service.user.presentation.response.UserPendingResponse;
import com.delivery_project.user_service.user.presentation.response.UserReinstateResponse;
import com.delivery_project.user_service.user.presentation.response.UserRejectResponse;
import com.delivery_project.user_service.user.presentation.response.UserSuspendResponse;
import com.delivery_project.user_service.user.presentation.response.UserUpdateMeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 사용자 관리 API 문서. 팀 컨벤션 19번(Swagger 컨벤션) 대로 Swagger 애너테이션을
 * 컨트롤러에서 분리했다.
 *
 * 전 API 공통이라 개별 메서드에 적지 않은 것 — Authorization 헤더가 없거나 만료/무효화된
 * 토큰이면 401 AUTH_TOKEN_INVALID, 아직 승인되지 않은 계정으로 호출하면 403 USER_NOT_APPROVED.
 */
@Tag(name = "User", description = "사용자 관리 API")
public interface UserApi {

	@Operation(summary = "내 정보 조회", description = "로그인한 본인의 계정 정보를 조회한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	ResponseEntity<SuccessResponse<UserDetailResponse>> getMe(@Parameter(hidden = true) UUID callerId);

	@Operation(summary = "내 정보 수정", description = "로그인한 본인의 이름/Slack ID를 수정한다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "409", description = "USER_DUPLICATE_SLACK_ID — 이미 다른 계정이 쓰는 Slack ID")
	})
	ResponseEntity<SuccessResponse<UserUpdateMeResponse>> updateMe(
			@Parameter(hidden = true) UUID callerId,
			UserUpdateMeRequest request
	);

	@Operation(
			summary = "사용자 목록 조회 · 검색",
			description = "MASTER 전용. 승인 상태/역할/소속 허브·업체로 필터링해 전체 사용자를 페이징 조회한다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공(결과 0건도 200)"),
			@ApiResponse(responseCode = "403", description = "READ_USER_FORBIDDEN — MASTER가 아님")
	})
	ResponseEntity<SuccessResponse<PageResponse<UserListResponse>>> getUsers(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "승인 상태 필터") ApprovalStatus approvalStatus,
			@Parameter(description = "역할 필터") Role role,
			@Parameter(description = "소속 허브 ID 필터") UUID hubId,
			@Parameter(description = "소속 업체 ID 필터") UUID companyId,
			Pageable pageable
	);

	@Operation(
			summary = "승인 대기자 목록 조회",
			description = """
					MASTER는 전체(또는 hubId로 필터링), HUB_MANAGER는 본인 담당 허브로 신청한
					대기자만 조회한다. COMPANY_MANAGER/DELIVERY_MANAGER는 호출할 수 없다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공(결과 0건도 200)"),
			@ApiResponse(responseCode = "403", description = "READ_USER_FORBIDDEN — COMPANY_MANAGER/DELIVERY_MANAGER는 조회 권한 없음")
	})
	ResponseEntity<SuccessResponse<PageResponse<UserPendingResponse>>> getPendingUsers(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "소속 허브 ID 필터(HUB_MANAGER는 본인 허브로 고정)") UUID hubId,
			Pageable pageable
	);

	@Operation(
			summary = "사용자 단건 조회",
			description = "MASTER 또는 본인만 조회할 수 있다. 권한이 없으면 대상 존재 여부와 무관하게 403을 반환한다(존재 여부 은닉)."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "403", description = "READ_USER_FORBIDDEN — MASTER도 본인도 아님"),
			@ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
	})
	ResponseEntity<SuccessResponse<UserDetailResponse>> getById(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "조회할 사용자 ID", required = true) UUID userId
	);

	@Operation(
			summary = "사용자 삭제",
			description = """
					MASTER 전용. 마지막 활성 MASTER는 삭제할 수 없다.

					대상이 DELIVERY_MANAGER면 Delivery Service 담당자 레코드도 함께 정리한다 —
					진행 중인 배송에 배정된 상태거나 Delivery Service 연동이 실패하면 삭제 자체가 막힌다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "403", description = "DELETE_USER_FORBIDDEN — MASTER가 아님"),
			@ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
			@ApiResponse(responseCode = "409",
					description = "LAST_MASTER_DELETE_FORBIDDEN — 마지막 활성 MASTER, "
							+ "DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY — 대상이 진행 중인 배송에 배정된 DELIVERY_MANAGER"),
			@ApiResponse(responseCode = "503", description = "DELIVERY_SERVICE_UNAVAILABLE — 대상이 DELIVERY_MANAGER인데 Delivery Service 연동 실패")
	})
	ResponseEntity<SuccessResponse<UserDeleteResponse>> delete(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "삭제할 사용자 ID", required = true) UUID userId
	);

	@Operation(
			summary = "회원가입 승인",
			description = "MASTER 또는 대상이 신청한 허브의 담당 HUB_MANAGER가 PENDING 상태의 가입 신청을 승인한다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "승인 성공"),
			@ApiResponse(responseCode = "403",
					description = "APPROVE_USER_FORBIDDEN — MASTER/담당 HUB_MANAGER가 아님, "
							+ "HUB_PERMISSION_DENIED — 담당 허브가 아닌 HUB_MANAGER"),
			@ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
			@ApiResponse(responseCode = "409",
					description = "USER_ALREADY_PROCESSED — 이미 승인/거절 처리됨, "
							+ "INVALID_STATE — 동시에 들어온 다른 승인/거절 요청과 충돌")
	})
	ResponseEntity<SuccessResponse<UserApproveResponse>> approve(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "승인할 사용자 ID", required = true) UUID userId
	);

	@Operation(
			summary = "회원가입 거절",
			description = "MASTER 또는 대상이 신청한 허브의 담당 HUB_MANAGER가 PENDING 상태의 가입 신청을 거절한다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "거절 성공"),
			@ApiResponse(responseCode = "403",
					description = "REJECT_USER_FORBIDDEN — MASTER/담당 HUB_MANAGER가 아님, "
							+ "HUB_PERMISSION_DENIED — 담당 허브가 아닌 HUB_MANAGER"),
			@ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
			@ApiResponse(responseCode = "409",
					description = "USER_ALREADY_PROCESSED — 이미 승인/거절 처리됨, "
							+ "INVALID_STATE — 동시에 들어온 다른 승인/거절 요청과 충돌")
	})
	ResponseEntity<SuccessResponse<UserRejectResponse>> reject(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "거절할 사용자 ID", required = true) UUID userId
	);

	@Operation(
			summary = "계정 정지",
			description = """
					MASTER 전용. 승인된(APPROVED) 계정만 정지할 수 있고, 마지막 활성 MASTER는 정지할
					수 없다. 정지 즉시 그 계정의 모든 기기 세션(Refresh Token)이 삭제되고 발급된
					Access Token도 무효화된다.

					대상이 DELIVERY_MANAGER면 Delivery Service 담당자도 비활성화해서 신규 배송
					배정에서 제외한다(진행 중인 배송은 그대로 완료됨) — 연동이 실패하면 정지 자체가 막힌다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "정지 성공"),
			@ApiResponse(responseCode = "403", description = "SUSPEND_USER_FORBIDDEN — MASTER가 아님"),
			@ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
			@ApiResponse(responseCode = "409",
					description = "USER_NOT_SUSPENDABLE — APPROVED 상태가 아님, "
							+ "LAST_MASTER_SUSPEND_FORBIDDEN — 마지막 활성 MASTER"),
			@ApiResponse(responseCode = "503", description = "DELIVERY_SERVICE_UNAVAILABLE — 대상이 DELIVERY_MANAGER인데 Delivery Service 연동 실패")
	})
	ResponseEntity<SuccessResponse<UserSuspendResponse>> suspend(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "정지할 사용자 ID", required = true) UUID userId
	);

	@Operation(
			summary = "계정 정지 해제",
			description = """
					MASTER 전용. 정지(SUSPENDED) 상태인 계정만 해제할 수 있다.

					대상이 DELIVERY_MANAGER면 Delivery Service 담당자도 다시 활성화해서 신규 배송
					배정 대상에 포함시킨다 — 연동이 실패하면 해제 자체가 막힌다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "정지 해제 성공"),
			@ApiResponse(responseCode = "403", description = "REINSTATE_USER_FORBIDDEN — MASTER가 아님"),
			@ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
			@ApiResponse(responseCode = "409", description = "USER_NOT_SUSPENDED — 정지 상태가 아님"),
			@ApiResponse(responseCode = "503", description = "DELIVERY_SERVICE_UNAVAILABLE — 대상이 DELIVERY_MANAGER인데 Delivery Service 연동 실패")
	})
	ResponseEntity<SuccessResponse<UserReinstateResponse>> reinstate(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(description = "정지 해제할 사용자 ID", required = true) UUID userId
	);
}
