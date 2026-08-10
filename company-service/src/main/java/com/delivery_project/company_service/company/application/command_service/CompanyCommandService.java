package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.pesistence_service.CompanyPersistenceService;
import com.delivery_project.company_service.company.application.port.HubPort;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.port.dto.HubInfo;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.delivery_project.company_service.global.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyCommandService {

    private final CompanyPersistenceService companyPersistenceService;
    private final UserPort userPort;
    private final HubPort hubPort;

    // [외부] 업체 생성 비즈니스 로직
    public CompanyCreateResult createCompany(CompanyCreateCommand companyCreateCommand) {

        log.info(
                "업체 생성 요청. callerId={}",
                companyCreateCommand.callerId()
        );

        /*
         * 업체 생성 검증
         * 1. 권한 검증
         *  - Master, 담당 Hub Manager만 가능
         *
         * 2. 존재 여부 검증
         *  - 실제 존재하는 Hub인지 확인
         */

        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyCreateCommand.callerId());

        // Master, 담당 Hub Manager만 가능하도록 검증
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
         * Hub Service를 통해 요청한 Hub의 존재 여부를 검증
         * 반환된 HubInfo는 현재 업체 수정 로직에서는 사용하지 않음
         */
        HubInfo hubInfo = hubPort.getHub(companyCreateCommand.hubId());

        // 실제 DB 저장 시점부터 트랜잭션 시작
        return companyPersistenceService.createCompany(
                companyCreateCommand.hubId(),
                companyCreateCommand.type(),
                companyCreateCommand.name(),
                companyCreateCommand.address()
        );
    }

    // [외부] 업체 수정 비즈니스 로직
    public CompanyUpdateResult updateCompany(CompanyUpdateCommand companyUpdateCommand) {

        log.info(
                "업체 수정 요청. callerId={}",
                companyUpdateCommand.callerId()
        );

        /*
         * 업체 수정 검증
         * 1. 권한 검증
         *  - Master, 담당 Hub Manager, 담당 Company Manager만 가능
         *
         * 2. 존재 여부 검증
         *  - 실제 존재하는 Company, Hub인지 확인
         */

        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyUpdateCommand.callerId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능하도록 검증
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
         * Hub Service를 통해 요청한 Hub의 존재 여부를 검증
         * 반환된 HubInfo는 현재 업체 수정 로직에서는 사용하지 않음
         */
        HubInfo hubInfo = hubPort.getHub(companyUpdateCommand.hubId());

        // 업체를 조회
        Company company = companyPersistenceService
                .getCompanyById(companyUpdateCommand.companyId())
                // 조회된 업체가 없는 경우
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 실제 DB 수정 시점부터 트랜잭션 시작
        return companyPersistenceService.updateCompany(
                company,
                companyUpdateCommand.hubId(),
                companyUpdateCommand.type(),
                companyUpdateCommand.name(),
                companyUpdateCommand.address()
        );
    }

    // [외부] 업체 삭제 비즈니스 로직
    public CompanyDeleteResult deleteCompany(CompanyDeleteCommand companyDeleteCommand) {

        log.info(
                "업체 삭제 요청. callerId={}",
                companyDeleteCommand.callerId()
        );

        /*
         * 업체 삭제 검증
         * 1. 존재 여부 검증
         *  - 실제 존재하는 Company인지 확인
         *  - 이때, 존재 여부가 권한이 없는 사용자에게 리소스가 노출되지 않도록 권한 오류와 동일하게 처리
         *
         * 2. 권한 검증
         *  - Master, 담당 Hub Manager만 가능
         *
         * 3. 존재 여부 검증
         *  - 실제 존재하는 Company, Hub인지 확인
         */

        /*
         * 업체 존재 여부를 확인
         * 존재하지 않는 경우에도 Company의 존재 여부가
         * 권한이 없는 사용자에게 노출되지 않도록 AUTH_FORBIDDEN으로 처리
         */
        Company company = companyPersistenceService
                .getCompanyById(companyDeleteCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyDeleteCommand.callerId());

        // Master, 담당 Hub Manager만 가능하도록 검증
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
         * Hub Service를 통해 요청한 Hub의 존재 여부를 검증
         * 반환된 HubInfo는 현재 업체 수정 로직에서는 사용하지 않음
         */
        HubInfo hubInfo = hubPort.getHub(company.getHubId());

        // 실제 DB 논리 삭제 시점부터 트랜잭션 시작
        return companyPersistenceService.deleteCompany(
                company,
                companyDeleteCommand.callerId()
        );
    }
}
