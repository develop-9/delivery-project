package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.presentation.request.ProductCreateRequest;
import com.delivery_project.company_service.company.presentation.request.ProductUpdateRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Product-Api", description = "Product 관련 API")
public interface ProductApi {

    @Operation(
            summary = "상품 생성",
            description = "Master, 담당 Hub Manager 또는 담당 Company Manager가 새로운 상품을 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "상품 생성 성공",
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
                    description = "상품 생성 권한이 없는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "요청한 Company가 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<ProductCreateResponse>> createProduct(

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "상품 생성 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductCreateRequest.class)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            ProductCreateRequest productCreateRequest
    );


    @Operation(
            summary = "상품 수정",
            description = "Master, 담당 Hub Manager 또는 담당 Company Manager가 상품의 정보를 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 수정 성공",
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
                    description = "상품 수정 권한이 없는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "요청한 상품이 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<ProductUpdateResponse>> updateProduct(

            @Parameter(
                    description = "수정할 상품 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID productId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "상품 수정 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductUpdateRequest.class)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            ProductUpdateRequest productUpdateRequest
    );

    @Operation(
            summary = "상품 삭제",
            description = "Master 또는 담당 Hub Manager가 상품을 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "상품 삭제 권한이 없는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "요청한 상품이 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<ProductDeleteResponse>> deleteProduct(
            @Parameter(
                    description = "삭제할 상품 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID productId
    );

    @Operation(
            summary = "상품 단건 조회",
            description = "인증된 모든 사용자가 상품의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "요청한 상품이 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<SuccessResponse<ProductGetResponse>> getProduct(
            @Parameter(
                    description = "조회할 상품 ID",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID productId
    );

    @Operation(
            summary = "상품 목록 조회 / 검색",
            description = "인증된 모든 사용자가 상품 목록을 페이지네이션 및 검색 조건에 따라 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 / 검색 성공",
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
    ResponseEntity<SuccessResponse<PageResponse<ProductSearchResponse>>> searchProduct(

            @Parameter(
                    description = "조회할 페이지 번호",
                    example = "0",
                    required = true
            )
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(
                    description = "페이지당 조회할 상품 수",
                    example = "10",
                    required = true
            )
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(
                    description = "정렬 기준 및 방향",
                    example = "createdAt,desc",
                    required = true
            )
            @RequestParam(defaultValue = "createdAt,desc") String sort,

            @Parameter(
                    description = "Company ID 검색 조건",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    required = false
            )
            @RequestParam(required = false) UUID companyId,

            @Parameter(
                    description = "상품명 검색 조건",
                    example = "노트북",
                    required = false
            )
            @RequestParam(required = false) String name,

            @Parameter(
                    description = "최소 상품 가격",
                    example = "10000",
                    required = false
            )
            @RequestParam(required = false) Integer minPrice,

            @Parameter(
                    description = "최대 상품 가격",
                    example = "1000000",
                    required = false
            )
            @RequestParam(required = false) Integer maxPrice
    );
}
