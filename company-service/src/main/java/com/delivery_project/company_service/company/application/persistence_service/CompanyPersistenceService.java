package com.delivery_project.company_service.company.application.persistence_service;

import com.delivery_project.company_service.company.application.port.OrderPort;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.repository.CompanyCommandRepository;
import com.delivery_project.company_service.company.domain.repository.ProductQueryRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyPersistenceService {

    private final CompanyCommandRepository companyCommandRepository;
    private final ProductQueryRepository productQueryRepository;
    private final OrderPort orderPort;

    /*
     * TODO: 업체 식별자 추가 후 Unique 제약 적용
     * 현재 name, hubId, type만으로는 업체를 유일하게 식별할 수 없음
     *
     * 추후 업체 등록번호 등 유일 식별 필드가 추가되고
     * 해당 필드의 유일성이 비즈니스 규칙으로 확정되면
     * DB Unique 제약을 적용하여 중복 등록 및 데이터 무결성을 보장
     */
    @Transactional
    public CompanyCreateResult createCompany(UUID hubId, CompanyType type, String name, String address) {
        // 업체 생성
        Company company = Company.create(
                hubId,
                type,
                name,
                address
        );

        // 업체 저장
        Company savedCompany = companyCommandRepository.save(company);

        log.info(
                "업체 생성 완료. companyId={}, createdBy={}",
                savedCompany.getId(),
                savedCompany.getCreatedBy()
        );

        // 결과값 반환
        return CompanyCreateResult.from(savedCompany.getId());
    }

    @Transactional
    public CompanyUpdateResult updateCompany(UUID companyId, UUID hubId, CompanyType type, String name, String address) {
        // 업체 조회
        Company company = validateCompany(companyId);

        // 업체 정보 수정
        company.update(
                hubId,
                type,
                name,
                address
        );

        // 수정된 내용 저장
        Company updatedCompany = companyCommandRepository.save(company);

        log.info(
                "업체 수정 완료. companyId={}, updatedBy={}",
                company.getId(),
                company.getUpdatedBy()
        );

        // 결과 반환
        return CompanyUpdateResult.from(updatedCompany.getId());
    }

    @Transactional
    public CompanyDeleteResult deleteCompany(UUID companyId, UUID callerId) {
        // 업체 조회
        Company company = validateCompany(companyId);

        productQueryRepository
                // 업체가 가지고 있는 상품들 조회
                .findByCompanyId(company.getId())
                .forEach(product -> {
                    // 업체에 소속된 상품의 재고 삭제 요청
                    orderPort.deleteInventory(product.getId());

                    // 업체에 소속된 상품 논리 삭제
                    product.delete(callerId);
                });

        // 업체 논리 삭제
        company.delete(callerId);

        // 삭제된 내용 저장
        Company deletedCompany = companyCommandRepository.save(company);

        log.info(
                "업체 논리 삭제 완료. companyId={}, deletedBy={}",
                company.getId(),
                company.getDeletedBy()
        );

        // 결과 반환
        return CompanyDeleteResult.from(deletedCompany.getId(), deletedCompany.getDeletedAt());
    }

    @Transactional(readOnly = true)
    public Optional<Company> getCompanyById(UUID companyId) {
        return companyCommandRepository.findById(companyId);
    }


    // Validation Check - 업체 조회
    private Company validateCompany(UUID companyId) {
        return companyCommandRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
