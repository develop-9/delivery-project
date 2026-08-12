package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryRouteStatusUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryRouteDetailResponse;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryRouteStatusUpdateResponse;
import com.delivery_project.delivery_service.global.response.ErrorResponse;
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

import java.util.UUID;

@Tag(
        name = "DeliveryRoute-Api",
        description = "배송 경로 조회 및 상태 변경 API"
)
public interface DeliveryRouteApi {

    @Operation(
            summary = "배송 경로 상태 변경",
            description = """
                    배송 경로의 상태를 변경합니다.
                    WAITING → IN_TRANSIT 변경 시 허브 배송 담당자를 자동 배정하고,
                    IN_TRANSIT → ARRIVED 변경 시 실제 이동 거리와 소요 시간을 저장합니다.
                    마지막 배송 경로가 도착하면 업체 배송 담당자를 자동 배정합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 경로 상태 변경 성공",
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
                    description = "배송 경로 상태 변경 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 경로 또는 배송 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            허용되지 않는 상태 변경,
                            이전 배송 경로 미완료,
                            이미 이동 중인 배송 경로 존재,
                            배정 가능한 배송 담당자 없음
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryRouteStatusUpdateResponse>
    updateDeliveryRouteStatus(
            @Parameter(
                    description = "상태를 변경할 배송 경로 ID",
                    required = true
            )
            @PathVariable UUID routeId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "배송 경로 상태 변경 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DeliveryRouteStatusUpdateRequest.class
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            DeliveryRouteStatusUpdateRequest request,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 경로 상세 조회",
            description = "배송 경로 ID를 기준으로 배송 경로 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 경로 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 경로 조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 경로가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryRouteDetailResponse> getDeliveryRoute(
            @Parameter(
                    description = "조회할 배송 경로 ID",
                    required = true
            )
            @PathVariable UUID routeId,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );
}