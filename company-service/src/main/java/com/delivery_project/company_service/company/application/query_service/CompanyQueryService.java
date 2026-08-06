package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.CompanySearchQuery;
import com.delivery_project.company_service.company.application.query.CompanyGetQuery;
import com.delivery_project.company_service.company.application.query.InternalCompanyGetQuery;
import com.delivery_project.company_service.company.application.result.InternalCompanyGetResult;
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

    // [외부] 업체 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public CompanyGetResult getCompany(CompanyGetQuery companyGetQuery) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyQueryRepository.findById(companyGetQuery.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        return CompanyGetResult.from(company);
    }

    // [외부] 업체 검색 비즈니스 로직
    @Transactional(readOnly = true)
    public CompanyGetAllResult getAllCompany(CompanySearchQuery companySearchQuery) {
        int page = pageValidator.validatePage(companySearchQuery.page());
        int size = pageValidator.normalizeSize(companySearchQuery.size());
        Sort sort = pageValidator.normalizeSort(companySearchQuery.sort());

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Company> companyPage =
                companyQueryRepository.search(
                        companySearchQuery.name(),
                        companySearchQuery.type(),
                        companySearchQuery.hubId(),
                        pageable
                );

        return CompanyGetAllResult.from(companyPage);
    }

    // [내부] 업체 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public InternalCompanyGetResult getCompanyForInternal(InternalCompanyGetQuery internalCompanyGetQuery) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyQueryRepository.findById(internalCompanyGetQuery.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        return InternalCompanyGetResult.from(company);
    }
}
