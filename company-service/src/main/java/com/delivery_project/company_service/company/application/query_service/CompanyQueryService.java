package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.command.CompanyGetAllCommand;
import com.delivery_project.company_service.company.application.command.CompanyGetCommand;
import com.delivery_project.company_service.company.application.support.pagination.PageValidator;
import com.delivery_project.company_service.company.application.result.CompanyGetAllResult;
import com.delivery_project.company_service.company.application.result.CompanyGetResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyQueryService {

    private final CompanyQueryRepository companyQueryRepository;
    private final PageValidator pageValidator;

    // 업체 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public CompanyGetResult getCompany(CompanyGetCommand companyGetCommand) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyQueryRepository.findById(companyGetCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        return CompanyGetResult.from(company);
    }

    // 업체 검색 비즈니스 로직
    @Transactional(readOnly = true)
    public CompanyGetAllResult getAllCompany(CompanyGetAllCommand companyGetAllCommand) {
        int page = pageValidator.validatePage(companyGetAllCommand.page());
        int size = pageValidator.normalizeSize(companyGetAllCommand.size());
        Sort sort = pageValidator.normalizeSort(companyGetAllCommand.sort());

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Company> companyPage =
                companyQueryRepository.search(
                        companyGetAllCommand.name(),
                        companyGetAllCommand.type(),
                        companyGetAllCommand.hubId(),
                        pageable
                );

        return CompanyGetAllResult.from(companyPage);
    }
}
