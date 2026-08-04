package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyRepository;
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

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyCreateResult createCompany(CompanyCreateCommand companyCreateCommand) {
        /*
        * TODO:
        *  Validation Check - 권한 검증
        *  Validation Check - hub_id를 통해 실제 존재하는 허브인지 확인
        */

        // 업체 생성
        Company company = Company.builder()
                .hubId(companyCreateCommand.hubId())
                .type(companyCreateCommand.type())
                .name(companyCreateCommand.name())
                .address(companyCreateCommand.address())
                .build();

        // 업체 저장
        Company savedCompany = companyRepository.save(company);

        log.info(
                "업체 생성 완료. companyId={}",
                savedCompany.getId()
        );

        return CompanyCreateResult.from(savedCompany.getId());
    }

    @Transactional
    public CompanyUpdateResult updateCompany(UUID companyId, CompanyUpdateCommand companyUpdateCommand) {

        // Validation Check - 업체 존재 여부 판단
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        /*
         * TODO:
         *  Validation Check - 권한 검증
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
                "업체 수정 완료. companyId={}",
                company.getId()
        );

        return CompanyUpdateResult.from(company.getId());
    }
}
