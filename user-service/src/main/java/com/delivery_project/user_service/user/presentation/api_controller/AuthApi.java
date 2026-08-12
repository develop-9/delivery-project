package com.delivery_project.user_service.user.presentation.api_controller;

import org.springframework.http.ResponseEntity;

import com.delivery_project.user_service.global.response.SuccessResponse;
import com.delivery_project.user_service.user.presentation.request.UserLoginRequest;
import com.delivery_project.user_service.user.presentation.request.UserRefreshRequest;
import com.delivery_project.user_service.user.presentation.request.UserSignupRequest;
import com.delivery_project.user_service.user.presentation.response.UserLoginResponse;
import com.delivery_project.user_service.user.presentation.response.UserRefreshResponse;
import com.delivery_project.user_service.user.presentation.response.UserSignupResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

/**
 * 인증/회원가입 API 문서. 팀 컨벤션 19번(Swagger 컨벤션) 대로 Swagger 애너테이션을
 * 컨트롤러에서 분리했다.
 *
 * signup/login/refresh는 로그인 없이 호출하는 API라 여기 없음. logout만 인증이 필요하고,
 * Authorization 헤더가 없거나 형식이 잘못되면 401 AUTH_TOKEN_INVALID를 반환한다.
 */
@Tag(name = "Auth", description = "인증/회원가입 API")
public interface AuthApi {

	@Operation(
			summary = "회원가입",
			description = """
					신규 계정을 PENDING 상태로 생성한다. MASTER 역할이면서 활성 MASTER가 한 명도
					없는 최초 가입인 경우에 한해 자동으로 APPROVED된다.

					역할별로 필수 소속이 다르다 — HUB_MANAGER는 hubId 필수, COMPANY_MANAGER는 companyId
					필수, MASTER는 둘 다 필요 없다. DELIVERY_MANAGER는 hubId를 여기서 강제하지 않는다 —
					배송 담당자가 특정 허브 소속인지(hubId 필요) 공용 허브 소속인지(hubId 없음)는
					Delivery Service가 담당자 타입에 따라 별도로 검증한다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "가입 성공(PENDING, 또는 최초 MASTER면 APPROVED)"),
			@ApiResponse(responseCode = "400",
					description = "INVALID_INPUT_VALUE — 필수값 누락/형식 오류, 또는 역할별 필수 소속(hubId/companyId) 누락"),
			@ApiResponse(responseCode = "404", description = "HUB_NOT_FOUND · COMPANY_NOT_FOUND — 존재하지 않는 hubId/companyId"),
			@ApiResponse(responseCode = "409", description = "USER_DUPLICATE_USERNAME · USER_DUPLICATE_SLACK_ID"),
			@ApiResponse(responseCode = "503", description = "HUB_SERVICE_UNAVAILABLE · COMPANY_SERVICE_UNAVAILABLE")
	})
	ResponseEntity<SuccessResponse<UserSignupResponse>> signup(UserSignupRequest request);

	@Operation(
			summary = "로그인",
			description = "username/password로 인증하고 Access/Refresh Token 쌍을 발급한다. 세션(기기)마다 독립된 토큰이 발급된다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그인 성공"),
			@ApiResponse(responseCode = "401", description = "AUTH_INVALID_CREDENTIALS — 아이디/비밀번호 불일치"),
			@ApiResponse(responseCode = "403", description = "USER_NOT_APPROVED — 아직 승인되지 않은 계정")
	})
	ResponseEntity<SuccessResponse<UserLoginResponse>> login(UserLoginRequest request);

	@Operation(
			summary = "토큰 재발급",
			description = """
					Refresh Token으로 새 Access/Refresh Token 쌍을 발급받는다. 같은 세션(기기)의
					토큰만 원자적으로 교체되고, 다른 기기의 세션은 영향받지 않는다.

					동시에 같은 토큰으로 두 번 이상 요청이 들어오면 하나만 성공하고 나머지는
					AUTH_TOKEN_EXPIRED로 응답한다(재로그인 유도)."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "재발급 성공"),
			@ApiResponse(responseCode = "401",
					description = "AUTH_TOKEN_INVALID — 서명 불일치/형식 오류/Access Token으로 시도, "
							+ "AUTH_TOKEN_EXPIRED — 만료되었거나 이미 로테이션/로그아웃된 토큰"),
			@ApiResponse(responseCode = "403", description = "USER_NOT_APPROVED — 그 사이 정지/삭제된 계정")
	})
	ResponseEntity<SuccessResponse<UserRefreshResponse>> refresh(UserRefreshRequest request);

	@Operation(
			summary = "로그아웃",
			description = """
					로그아웃을 요청한 기기의 세션만 끝낸다 — 다른 기기의 세션은 그대로 유지된다.

					그 세션의 Refresh Token을 즉시 삭제하고, 아직 만료 전인 Access Token도
					Gateway가 확인하는 세션 블랙리스트에 등록해 자연 만료 전에 차단되게 한다."""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "로그아웃 성공"),
			@ApiResponse(responseCode = "401", description = "AUTH_TOKEN_INVALID — Authorization 헤더 없음/형식 오류")
	})
	ResponseEntity<Void> logout(
			@Parameter(hidden = true) UUID callerId,
			@Parameter(hidden = true) String authorizationHeader
	);
}
