package com.delivery_project.company_service.company.presentation.internal_controller;

import com.delivery_project.company_service.company.presentation.response.InternalCompanyGetResponse;
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

@Tag(name = "Company", description = "Company 관련 내부 API")
public interface CompanyInternal {

    @Operation(
            summary = "업체 단건 조회",
            description = "주문 시 업체 하나의 정보를 조회합니다."
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
    ResponseEntity<SuccessResponse<InternalCompanyGetResponse>> getCompany(
            @Parameter(
                    description = "조회할 업체 ID",
                    required = true
            )
            @PathVariable UUID companyId
    );
}
