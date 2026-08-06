package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.presentation.request.ProductCreateRequest;
import com.delivery_project.company_service.company.presentation.request.ProductUpdateRequest;
import com.delivery_project.company_service.company.presentation.response.ProductCreateResponse;
import com.delivery_project.company_service.company.presentation.response.ProductUpdateResponse;
import com.delivery_project.company_service.global.response.ErrorResponse;
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
}
