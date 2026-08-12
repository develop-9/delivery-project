package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerCreateRequest;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.*;
import com.delivery_project.delivery_service.global.response.ErrorResponse;
import com.delivery_project.delivery_service.global.response.PageResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import com.delivery_project.delivery_service.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(
        name = "DeliveryManager-Api",
        description = "배송 담당자 생성, 조회, 수정 및 삭제 API"
)
public interface DeliveryManagerApi {

    @Operation(
            summary = "배송 담당자 생성",
            description = """
                    새로운 배송 담당자를 등록합니다.
                    담당자 타입과 배정 범위에 따라 라운드로빈 순번이 자동으로 부여됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "배송 담당자 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 필수값 누락",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 담당자 생성 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 허브 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 등록된 배송 담당자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryManagerCreateResponse> createDeliveryManager(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "배송 담당자 생성 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DeliveryManagerCreateRequest.class
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            DeliveryManagerCreateRequest request,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 담당자 상세 조회",
            description = "배송 담당자 ID를 기준으로 배송 담당자의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 담당자 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 담당자 조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 담당자가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryManagerDetailResponse> getDeliveryManager(
            @Parameter(
                    description = "조회할 배송 담당자 ID",
                    required = true
            )
            @PathVariable UUID managerId,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 담당자 목록 조회",
            description = "권한에 따라 배송 담당자 목록을 페이지 단위로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 담당자 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 페이지 조건",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 담당자 목록 조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<PageResponse<DeliveryManagerListResponse>>
    getDeliveryManagers(
            @Parameter(
                    description = "조회할 페이지 번호",
                    example = "0",
                    required = false
            )
            @RequestParam
            int page,

            @Parameter(
                    description = "페이지당 조회할 배송 담당자 수",
                    example = "10",
                    required = false
            )
            @RequestParam
            int size,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "내 배송 담당자 정보 조회",
            description = "로그인한 사용자의 배송 담당자 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "내 배송 담당자 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 담당자 본인 조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 담당자 정보가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryManagerDetailResponse> getMyDeliveryManager(
            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 담당자 수정",
            description = """
                    배송 담당자의 타입 또는 소속 허브 정보를 수정합니다.
                    배송 중인 담당자는 수정할 수 없습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 담당자 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 담당자 타입과 허브 조건 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 담당자 수정 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 담당자 또는 허브 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "배송 중인 담당자는 수정할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryManagerUpdateResponse> updateDeliveryManager(
            @Parameter(
                    description = "수정할 배송 담당자 ID",
                    required = true
            )
            @PathVariable UUID managerId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "배송 담당자 수정 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DeliveryManagerUpdateRequest.class
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            DeliveryManagerUpdateRequest request,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 담당자 삭제",
            description = """
                    배송 담당자를 논리 삭제합니다.
                    배송 중인 담당자는 삭제할 수 없습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 담당자 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 담당자 삭제 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 담당자가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "배송 중이거나 진행 중인 배정이 존재하여 삭제할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryManagerDeleteResponse> deleteDeliveryManager(
            @Parameter(
                    description = "삭제할 배송 담당자 ID",
                    required = true
            )
            @PathVariable UUID managerId,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );
}