package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.command_service.CompanyCommandService;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.presentation.request.CompanyCreateRequest;
import com.delivery_project.company_service.company.presentation.request.CompanyUpdateRequest;
import com.delivery_project.company_service.company.presentation.response.CompanyCreateResponse;
import com.delivery_project.company_service.company.presentation.response.CompanyUpdateResponse;
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

    @PostMapping
    @Override
    public ResponseEntity<SuccessResponse<CompanyCreateResponse>> createCompany(
            @RequestBody @Valid CompanyCreateRequest companyCreateRequest
    ) {
        CompanyCreateCommand companyCreateCommand = CompanyCreateCommand.from(companyCreateRequest);
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
        CompanyUpdateCommand companyUpdateCommand = CompanyUpdateCommand.from(companyUpdateRequest);
        CompanyUpdateResult companyUpdateResult = companyCommandService.updateCompany(companyId, companyUpdateCommand);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                CompanyUpdateResponse.from(
                                        companyUpdateResult
                                ))
                );
    }
}
