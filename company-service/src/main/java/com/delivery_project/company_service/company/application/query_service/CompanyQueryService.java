package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.command.CompanyGetCommand;
import com.delivery_project.company_service.company.application.result.CompanyGetResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyQueryService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public CompanyGetResult getCompany(CompanyGetCommand companyGetCommand) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyRepository.findById(companyGetCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        return CompanyGetResult.from(company);
    }
}
