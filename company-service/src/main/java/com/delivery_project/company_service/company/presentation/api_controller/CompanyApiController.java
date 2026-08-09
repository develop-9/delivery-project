package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.*;
import com.delivery_project.company_service.company.application.command_service.CompanyCommandService;
import com.delivery_project.company_service.company.application.query.CompanySearchQuery;
import com.delivery_project.company_service.company.application.query.CompanyGetQuery;
import com.delivery_project.company_service.company.application.query_service.CompanyQueryService;
import com.delivery_project.company_service.company.application.result.*;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.presentation.request.*;
import com.delivery_project.company_service.company.presentation.response.*;
import com.delivery_project.company_service.global.response.PageResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyApiController implements CompanyApi {

    private final CompanyCommandService companyCommandService;
    private final CompanyQueryService companyQueryService;

    @PostMapping
    @Override
    public ResponseEntity<SuccessResponse<CompanyCreateResponse>> createCompany(
            @RequestBody @Valid CompanyCreateRequest companyCreateRequest
    ) {
        CompanyCreateCommand companyCreateCommand = companyCreateRequest.toCommand();
        CompanyCreateResult companyCreateResult = companyCommandService.createCompany(companyCreateCommand);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        SuccessResponse.success(
                                CompanyCreateResponse.from(
                                        companyCreateResult
                                ))
                );
    }

    @PutMapping("/{companyId}")
    @Override
    public ResponseEntity<SuccessResponse<CompanyUpdateResponse>> updateCompany(
            @PathVariable UUID companyId,
            @RequestBody @Valid CompanyUpdateRequest companyUpdateRequest
    ) {
        CompanyUpdateCommand companyUpdateCommand = companyUpdateRequest.toCommand(companyId);
        CompanyUpdateResult companyUpdateResult = companyCommandService.updateCompany(companyUpdateCommand);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                CompanyUpdateResponse.from(
                                        companyUpdateResult
                                ))
                );
    }

    @DeleteMapping("/{companyId}")
    @Override
    public ResponseEntity<SuccessResponse<CompanyDeleteResponse>> deleteCompany(
            @PathVariable UUID companyId
    ) {
        CompanyDeleteCommand companyDeleteCommand = new CompanyDeleteRequest().toCommand(companyId);
        CompanyDeleteResult companyDeleteResult = companyCommandService.deleteCompany(companyDeleteCommand);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                CompanyDeleteResponse.from(
                                        companyDeleteResult
                                ))
                );
    }

    @GetMapping("/{companyId}")
    @Override
    public ResponseEntity<SuccessResponse<CompanyGetResponse>> getCompany(
            @PathVariable UUID companyId
    ) {
        CompanyGetQuery companyGetQuery = new CompanyGetRequest().toCommand(companyId);
        CompanyGetResult companyGetResult = companyQueryService.getCompany(companyGetQuery);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                CompanyGetResponse.from(
                                        companyGetResult
                                ))
                );
    }

    @GetMapping
    @Override
    public ResponseEntity<SuccessResponse<PageResponse<CompanySearchDataResponse>>> getAllCompany(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) CompanyType companyType,
            @RequestParam(required = false) UUID hubId
    ) {
        CompanySearchQuery companySearchQuery =
                new CompanySearchRequest().toCommand(
                        page,
                        size,
                        sort,
                        companyName,
                        companyType,
                        hubId
                );

        CompanySearchResult companySearchResult = companyQueryService.getAllCompany(companySearchQuery);

        PageResponse<CompanySearchDataResponse> pageResponse =
                PageResponse.of(
                        companySearchResult.content(),
                        companySearchResult.page(),
                        companySearchResult.size(),
                        companySearchResult.totalElements(),
                        companySearchResult.totalPages(),
                        CompanySearchDataResponse::from
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(pageResponse)
                );
    }
}
