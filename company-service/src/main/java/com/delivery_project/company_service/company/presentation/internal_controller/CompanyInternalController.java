package com.delivery_project.company_service.company.presentation.internal_controller;

import com.delivery_project.company_service.company.application.query.InternalCompanyGetQuery;
import com.delivery_project.company_service.company.application.query_service.CompanyQueryService;
import com.delivery_project.company_service.company.application.result.InternalCompanyGetResult;
import com.delivery_project.company_service.company.presentation.request.InternalCompanyGetRequest;
import com.delivery_project.company_service.company.presentation.response.InternalCompanyGetResponse;
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
    public ResponseEntity<SuccessResponse<InternalCompanyGetResponse>> getCompany(
            @PathVariable UUID companyId
    ) {
        InternalCompanyGetQuery internalCompanyGetQuery = new InternalCompanyGetRequest().toCommand(companyId);
        InternalCompanyGetResult internalCompanyGetResult = companyQueryService.getCompanyForInternal(internalCompanyGetQuery);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                InternalCompanyGetResponse.from(internalCompanyGetResult)
                        )
                );
    }
}
