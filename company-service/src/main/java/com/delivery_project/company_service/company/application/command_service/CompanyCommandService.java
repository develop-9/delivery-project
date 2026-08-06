package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyCommandRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyCommandService {

    private final CompanyCommandRepository companyCommandRepository;

    // 업체 저장 비즈니스 로직
    @Transactional
    public CompanyCreateResult createCompany(CompanyCreateCommand companyCreateCommand) {
        /*
        * TODO:
        *  Validation Check - 권한 검증 (Master와 담당 Hub Manager만 가능)
        *  Validation Check - hub_id를 통해 실제 존재하는 허브인지 확인
        */

        // 업체 생성
        Company company = Company.create(
                companyCreateCommand.hubId(),
                companyCreateCommand.type(),
                companyCreateCommand.name(),
                companyCreateCommand.address()
        );

        // 업체 저장
        Company savedCompany = companyCommandRepository.save(company);

        log.info(
                "업체 생성 완료. companyId={}, createdBy={}",
                savedCompany.getId(),
                savedCompany.getCreatedBy()
        );

        return CompanyCreateResult.from(savedCompany.getId());
    }

    // 업체 수정 비즈니스 로직
    @Transactional
    public CompanyUpdateResult updateCompany(CompanyUpdateCommand companyUpdateCommand) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyCommandRepository.findById(companyUpdateCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        /*
         * TODO:
         *  Validation Check - 권한 검증 (Master와 담당 Hub Manager, 담당 Company Manager만 가능)
         *  Validation Check - hub_id를 통해 실제 존재하는 허브인지 확인
         */

        // 업체 정보 수정
        company.update(
                companyUpdateCommand.hubId(),
                companyUpdateCommand.type(),
                companyUpdateCommand.name(),
                companyUpdateCommand.address()
        );

        log.info(
                "업체 수정 완료. companyId={}, updatedBy={}",
                company.getId(),
                company.getUpdatedBy()
        );

        return CompanyUpdateResult.from(company.getId());
    }

    // 업체 삭제 비즈니스 로직
    @Transactional
    public CompanyDeleteResult deleteCompany(CompanyDeleteCommand companyDeleteCommand) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyCommandRepository.findById(companyDeleteCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        /*
         * TODO:
         *  Validation Check - 권한 검증 (Master와 담당 Hub Manager만 가능)
         */

        /*
         * TODO:
         *  권한이 통과된 사용자의 UUID 기입
         *  현재는 임의로 생성된 UUID를 작성
         */

        /*
         * TODO:
         *  업체가 가지고 있는 상품들 삭제
         *  허브에 존재하는 상품들 삭제
         */

        company.delete(UUID.fromString("12345678-1234-5678-1234-123456789123"));

        log.info(
                "업체 논리 삭제 완료. companyId={}, deletedBy={}",
                company.getId(),
                company.getDeletedBy()
        );

        return CompanyDeleteResult.from(company.getId(), company.getDeletedAt());
    }
}
