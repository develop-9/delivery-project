package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyCommandService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyCreateResult createCompany(CompanyCreateCommand companyCreateCommand) {
        /*
        * TODO:
        *  Validation Check - hub_id를 통해 실제 존재하는 허브인지 확인
        */

        Company company = Company.builder()
                .hubId(companyCreateCommand.hubId())
                .type(companyCreateCommand.type())
                .name(companyCreateCommand.name())
                .address(companyCreateCommand.address())
                .build();

        Company savedCompany = companyRepository.save(company);

        log.info(
                "업체 생성 완료. companyId={}",
                savedCompany.getId()
        );

        return CompanyCreateResult.from(savedCompany.getId());
    }
}
