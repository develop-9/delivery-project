package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.CompanySearchQuery;
import com.delivery_project.company_service.company.application.query.CompanyGetQuery;
import com.delivery_project.company_service.company.application.query.InternalCompanyGetQuery;
import com.delivery_project.company_service.company.application.result.InternalCompanyGetResult;
import com.delivery_project.company_service.company.application.support.pagination.PageValidator;
import com.delivery_project.company_service.company.application.result.CompanySearchResult;
import com.delivery_project.company_service.company.application.result.CompanyGetResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyQueryService {

    private final CompanyQueryRepository companyQueryRepository;
    private final PageValidator pageValidator;

    // [외부] 업체 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public CompanyGetResult getCompany(CompanyGetQuery companyGetQuery) {

        log.info(
                "업체 단건 조회 요청. callerId={}, companyId={}",
                companyGetQuery.callerId(),
                companyGetQuery.companyId()
        );

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        // 업체가 존재하는지 확인
        Company company = validateCompany(companyGetQuery.companyId());

        // 결과 반환
        return CompanyGetResult.from(company);
    }

    // [외부] 업체 검색 비즈니스 로직
    @Transactional(readOnly = true)
    public CompanySearchResult getAllCompany(CompanySearchQuery companySearchQuery) {

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        // page, size, sort가 올바른지 확인
        int page = pageValidator.validatePage(companySearchQuery.page());
        int size = pageValidator.normalizeSize(companySearchQuery.size());
        Sort sort = pageValidator.normalizeSort(companySearchQuery.sort());

        // Pageable 생성
        Pageable pageable = PageRequest.of(page, size, sort);

        // 업체 검색
        Page<Company> companyPage =
                companyQueryRepository.search(
                        companySearchQuery.name(),
                        companySearchQuery.type(),
                        companySearchQuery.hubId(),
                        pageable
                );

        log.info(
                "업체 목록 조회 완료. callerId={}, page={}, size={}, totalElements={}, totalPages={}",
                companySearchQuery.callerId(),
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages()
        );

        // 결과 반환
        return CompanySearchResult.from(companyPage);
    }

    // [내부] 업체 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public InternalCompanyGetResult getCompanyForInternal(InternalCompanyGetQuery internalCompanyGetQuery) {

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        // 업체가 존재하는지 확인
        Company company = validateCompany(internalCompanyGetQuery.companyId());

        log.info("[내부] 업체 단건 조회 성공. companyId={}",
                company.getId()
        );

        // 결과 반환
        return InternalCompanyGetResult.from(company);
    }


    // Validation Check - 업체 존재 여부 판단
    private Company validateCompany(UUID companyId) {
        return companyQueryRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
