package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyCommandRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.delivery_project.company_service.global.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyCommandService {

    private final CompanyCommandRepository companyCommandRepository;
    private final UserPort userPort;

    // [외부] 업체 저장 비즈니스 로직
    @Transactional
    public CompanyCreateResult createCompany(CompanyCreateCommand companyCreateCommand) {

        log.info(
                "업체 생성 요청. callerId={}",
                companyCreateCommand.callerId()
        );

        // 업체 생성 권한 검증
        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyCreateCommand.callerId());

        // Master, 담당 Hub Manager만 가능
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                companyCreateCommand.hubId()
                        )
                );

        // 권한이 없을 경우 오류 반환
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        /*
         * TODO:
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

        // 결과 반환
        return CompanyCreateResult.from(savedCompany.getId());
    }

    // [외부] 업체 수정 비즈니스 로직
    @Transactional
    public CompanyUpdateResult updateCompany(CompanyUpdateCommand companyUpdateCommand) {

        log.info(
                "업체 수정 요청. callerId={}",
                companyUpdateCommand.callerId()
        );

        // 업체가 존재하는지 확인
        Company company = validateCompany(companyUpdateCommand.companyId());

        // 업체 수정 권한 검증
        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyUpdateCommand.callerId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                companyUpdateCommand.hubId()
                        )
                )
                        || (
                        callerInfo.role() == Role.COMPANY_MANAGER
                                && Objects.equals(
                                callerInfo.companyId(),
                                companyUpdateCommand.companyId()
                        )
                );

        // 권한이 없을 경우 오류 반환
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        /*
         * TODO:
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

        // 결과 반환
        return CompanyUpdateResult.from(company.getId());
    }

    // [외부] 업체 삭제 비즈니스 로직
    @Transactional
    public CompanyDeleteResult deleteCompany(CompanyDeleteCommand companyDeleteCommand) {

        log.info(
                "업체 삭제 요청. callerId={}",
                companyDeleteCommand.callerId()
        );

        // 업체가 존재하는지 확인
        Company company = validateCompany(companyDeleteCommand.companyId());

        // 업체 생성 권한 검증
        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyDeleteCommand.callerId());

        // Master, 담당 Hub Manager만 가능
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                company.getHubId()
                        )
                );

        // 권한이 없을 경우 오류 반환
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        /*
         * TODO:
         *  업체가 가지고 있는 상품들 삭제
         *  허브에 존재하는 상품들 삭제
         */

        // 업체 논리 삭제
        company.delete(companyDeleteCommand.callerId());

        log.info(
                "업체 논리 삭제 완료. companyId={}, deletedBy={}",
                company.getId(),
                company.getDeletedBy()
        );

        // 결과 반환
        return CompanyDeleteResult.from(company.getId(), company.getDeletedAt());
    }


    // Validation Check - 업체 존재 여부 판단
    private Company validateCompany(UUID companyId) {
        return companyCommandRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }
}
