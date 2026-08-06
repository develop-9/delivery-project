package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.presentation.request.CompanyCreateRequest;
import com.delivery_project.company_service.company.presentation.request.CompanyUpdateRequest;
import com.delivery_project.company_service.company.presentation.response.*;
import com.delivery_project.company_service.global.response.ErrorResponse;
import com.delivery_project.company_service.global.response.PageResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Company-Api", description = "Company 관련 API")
public interface CompanyApi {

    @Operation(
            summary = "업체 생성",
            description = "Master 또는 담당 Hub Manager가 새로운 업체를 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "업체 생성 성공",
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
                    description = "업체 생성 권한이 없는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "요청한 Hub가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<CompanyCreateResponse>> createCompany(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "업체 생성 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyCreateRequest.class)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            CompanyCreateRequest companyCreateRequest
    );

    @Operation(
            summary = "업체 수정",
            description = "Master, 담당 Hub Manager 또는 본인 Company Manager가 업체 정보를 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "업체 수정 성공",
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
                    description = "업체 수정 권한이 없는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "업체가 존재하지 않음 또는 요청한 Hub가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<CompanyUpdateResponse>> updateCompany(
            @Parameter(
                    description = "수정할 업체 ID",
                    required = true
            )
            @PathVariable UUID companyId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "업체 수정 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompanyUpdateRequest.class)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            CompanyUpdateRequest companyUpdateRequest
    );

    @Operation(
            summary = "업체 삭제",
            description = "Master, 담당 Hub Manager가 업체를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "업체 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "업체 수정 권한이 없는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "업체가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<CompanyDeleteResponse>> deleteCompany(
            @Parameter(
                    description = "삭제할 업체 ID",
                    required = true
            )
            @PathVariable UUID companyId
    );

    @Operation(
            summary = "업체 조회",
            description = "모든 유저가 업체 하나의 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "업체 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "업체가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<CompanyGetResponse>> getCompany(
            @Parameter(
                    description = "조회할 업체 ID",
                    required = true
            )
            @PathVariable UUID companyId
    );

    @Operation(
            summary = "업체 검색",
            description = "모든 사용자가 업체 목록을 페이지네이션 및 검색 조건에 따라 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "업체 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 잘못된 검색 조건",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<PageResponse<CompanyGetAllDataResponse>>> getAllCompany(
            @Parameter(
                    description = "조회할 페이지 번호",
                    example = "0",
                    required = false
            )
            @RequestParam Integer page,

            @Parameter(
                    description = "페이지당 조회할 업체 수",
                    example = "10",
                    required = false
            )
            @RequestParam Integer size,

            @Parameter(
                    description = "정렬 기준",
                    example = "createdAt,desc",
                    required = false
            )
            @RequestParam(required = false) String sort,

            @Parameter(
                    description = "업체명 검색 조건",
                    example = "삼성",
                    required = false
            )
            @RequestParam(required = false) String companyName,

            @Parameter(
                    description = "업체 유형 검색 조건",
                    example = "PRODUCER",
                    required = false
            )
            @RequestParam(required = false) CompanyType companyType,

            @Parameter(
                    description = "Hub ID 검색 조건",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    required = false
            )
            @RequestParam(required = false) UUID hubId
    );
}
