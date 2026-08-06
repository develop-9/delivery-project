package com.delivery_project.company_service.company.presentation.internal_controller;

import com.delivery_project.company_service.company.application.command.CompanyGetCommand;
import com.delivery_project.company_service.company.application.query_service.CompanyQueryService;
import com.delivery_project.company_service.company.application.result.CompanyGetForInternalResult;
import com.delivery_project.company_service.company.presentation.response.CompanyGetForInternalResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/companies")
@RequiredArgsConstructor
public class CompanyInternalController implements CompanyInternal {

    private final CompanyQueryService companyQueryService;

    @GetMapping("/{companyId}")
    @Override
    public ResponseEntity<SuccessResponse<CompanyGetForInternalResponse>> getCompany(
            @PathVariable UUID companyId
    ) {
        CompanyGetCommand companyGetCommand = new CompanyGetCommand(companyId);
        CompanyGetForInternalResult companyGetForInternalResult = companyQueryService.getCompanyForInternal(companyGetCommand);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                CompanyGetForInternalResponse.from(companyGetForInternalResult)
                        )
                );
    }
}
