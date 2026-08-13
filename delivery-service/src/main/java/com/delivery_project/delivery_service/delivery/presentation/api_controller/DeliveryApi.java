package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryStatusUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryUpdateRequest;
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

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Delivery-Api",
        description = "배송 조회, 수정, 삭제 및 상태 변경 API"
)
public interface DeliveryApi {

    @Operation(
            summary = "배송 상태 변경",
            description = """
                    배송의 진행 상태를 변경합니다.
                    업체 배송 시작 시 HUB_ARRIVED → DELIVERING,
                    배송 완료 시 DELIVERING → COMPLETED 상태로 변경합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 상태 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 배송 상태 또는 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 상태 변경 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 또는 배송 담당자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않는 상태 변경 또는 업체 배송 담당자 상태 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryStatusUpdateResponse> updateDeliveryStatus(
            @Parameter(
                    description = "상태를 변경할 배송 ID",
                    required = true
            )
            @PathVariable UUID deliveryId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 배송 상태",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DeliveryStatusUpdateRequest.class
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            DeliveryStatusUpdateRequest request,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 상세 조회",
            description = "배송 ID를 기준으로 배송 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 정보가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryDetailResponse> getDelivery(
            @Parameter(
                    description = "조회할 배송 ID",
                    required = true
            )
            @PathVariable UUID deliveryId,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 목록 조회",
            description = """
                    배송 목록을 페이지 단위로 조회합니다.
                    주문 ID, 배송 상태, 출발 허브, 도착 허브,
                    업체 배송 담당자를 조건으로 검색할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 검색 또는 페이지 조건",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 목록 조회 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<PageResponse<DeliveryListResponse>> getDeliveries(
            @Parameter(
                    description = "주문 ID 검색 조건",
                    required = false
            )
            @RequestParam(required = false)
            UUID orderId,

            @Parameter(
                    description = "배송 상태 검색 조건",
                    example = "PENDING",
                    required = false
            )
            @RequestParam(required = false)
            DeliveryStatus status,

            @Parameter(
                    description = "출발 허브 ID 검색 조건",
                    required = false
            )
            @RequestParam(required = false)
            UUID departureHubId,

            @Parameter(
                    description = "도착 허브 ID 검색 조건",
                    required = false
            )
            @RequestParam(required = false)
            UUID destinationHubId,

            @Parameter(
                    description = "업체 배송 담당자 ID 검색 조건",
                    required = false
            )
            @RequestParam(required = false)
            UUID companyDeliveryManagerId,

            @Parameter(
                    description = "조회할 페이지 번호",
                    example = "0",
                    required = false
            )
            @RequestParam
            int page,

            @Parameter(
                    description = "페이지당 조회할 배송 수",
                    example = "10",
                    required = false
            )
            @RequestParam
            int size,

            @Parameter(
                    description = "정렬 기준",
                    example = "createdAt",
                    required = false
            )
            @RequestParam
            String sortBy,

            @Parameter(
                    description = "정렬 방향",
                    example = "desc",
                    required = false
            )
            @RequestParam
            String direction,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 경로 목록 조회",
            description = "배송 ID를 기준으로 해당 배송의 전체 배송 경로를 순서대로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 경로 목록 조회 성공",
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
                    description = "배송 정보가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<List<DeliveryRouteDetailResponse>> getDeliveryRoutes(
            @Parameter(
                    description = "배송 경로를 조회할 배송 ID",
                    required = true
            )
            @PathVariable UUID deliveryId,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 정보 수정",
            description = """
                    배송 주소 또는 수령인 정보를 수정합니다.
                    수정 가능한 배송 상태에서만 변경할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 정보 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 수정 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 또는 수령인 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "현재 배송 상태에서는 수정할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryUpdateResponse> updateDelivery(
            @Parameter(
                    description = "수정할 배송 ID",
                    required = true
            )
            @PathVariable UUID deliveryId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "배송 수정 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DeliveryUpdateRequest.class
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            DeliveryUpdateRequest request,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );

    @Operation(
            summary = "배송 삭제",
            description = """
                    배송 정보를 논리 삭제합니다.
                    삭제 가능한 배송 상태에서만 삭제할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "배송 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "배송 삭제 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "배송 정보가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "현재 배송 상태에서는 삭제할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    SuccessResponse<DeliveryDeleteResponse> deleteDelivery(
            @Parameter(
                    description = "삭제할 배송 ID",
                    required = true
            )
            @PathVariable UUID deliveryId,

            @Parameter(hidden = true)
            JwtPrincipal principal
    );
}