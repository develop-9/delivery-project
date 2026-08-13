package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.persistence_service.CompanyPersistenceService;
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
         * 업체 생성 검증 순서
         *
         * 1. 요청자 정보 조회
         *
         * 2. 생성 대상 Company의 소속 Hub 존재 여부 확인
         *  - Company는 실제 존재하는 Hub에 소속되어 있어야 함
         *  - Hub가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Hub 존재 여부가 외부에 노출되지 않도록 함
         *
         * 3. Company 생성 권한 검증
         *  - Master: 모든 Company 생성 가능
         *  - Hub Manager: 담당 Hub의 Company만 생성 가능
         */

        // 1. 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyCreateCommand.callerId());

        // 2. 생성 대상 Company의 소속 Hub 존재 여부 확인
        HubInfo hubInfo = hubPort.validateHub(companyCreateCommand.hubId());

        // 3. Company 생성 권한 검증
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                hubInfo.hubId()
                        )
                );

        // 셍성 권한이 없는 경우
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

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
         * 업체 수정 검증 순서
         *
         * 1. 요청자 정보 조회
         *
         * 2. 수정 대상 Company 존재 여부 확인
         *  - 실제 존재하는 Company인지 확인
         *  - Company가 존재하지 않는 경우 AUHT_FORBIDDEN으로 처리하여
         *    Company 존재 여부가 외부에 노출되지 않도록 함
         *
         * 3. 기존 Company의 소속 Hub 존재 여부 확인
         *  - Company는 실제 존재하는 Hub에 소속되어 있어야 함
         *  - Hub가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Hub 존재 여부가 외부에 노출되지 않도록 함
         *
         * 4. Company 수정 권한 검증
         *  - Master: 모든 Company 수정 가능
         *  - Hub Manager: 담당 Hub의 Company만 수정 가능
         *  - Company Manager: 담당 Company만 수정 가능
         *
         * 5. Company의 소속 Hub 변경 여부 확인
         *  - 소속 Hub 변경은 Master만 가능
         *  - 변경 대상 Hub가 실제 존재하는지 확인
         */

        // 1. 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyUpdateCommand.callerId());

        // 2. 수정 대상 Company 존재 여부 확인
        Company company = companyPersistenceService
                .getCompanyById(companyUpdateCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 3. 기존 Company의 소속 Hub 존재 여부 확인
        HubInfo hubInfo = hubPort.validateHub(company.getHubId());

        // 4. Company 수정 권한 검증
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                hubInfo.hubId()
                        )
                )
                        || (
                        callerInfo.role() == Role.COMPANY_MANAGER
                                && Objects.equals(
                                callerInfo.companyId(),
                                company.getId()
                        )
                );

        // 수정 권한이 없는 경우
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        // 5. Company의 소속 Hub 변경 여부 확인
        boolean isHubChanged =
                !Objects.equals(
                        company.getHubId(),
                        companyUpdateCommand.hubId()
                );

        // 소속 Hub가 변경된 경우
        if (isHubChanged) {

            // 업체의 소속 Hub 변경은 Master만 가능
            if (callerInfo.role() != Role.MASTER) {
                throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
            }

            // 변경 대상 Hub가 존재하는지 확인
            hubPort.validateHub(companyUpdateCommand.hubId());
        }

        // 실제 DB 수정 시점부터 트랜잭션 시작
        return companyPersistenceService.updateCompany(
                company.getId(),
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
         * 업체 삭제 검증 순서
         *
         * 1. 요청자 정보 조회
         *
         * 2. 삭제 대상 Company 존재 여부 확인
         *  - 실제 존재하는 Company인지 확인
         *  - Company가 존재하지 않는 경우 AUHT_FORBIDDEN으로 처리하여
         *    Company 존재 여부가 외부에 노출되지 않도록 함
         *
         * 3. 기존 Company의 소속 Hub 존재 여부 확인
         *  - 삭제 대상 Company가 현재 접근 가능한 Hub에 소속되어 있는지 확인
         *  - Hub가 삭제된 경우 소속된 Company에도 접근할 수 없음
         *  - Hub가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Hub 존재 여부가 외부에 노출되지 않도록 함
         *
         * 4. Company 삭제 권한 검증
         *  - Master: 모든 Company 삭제 가능
         *  - Hub Manager: 담당 Hub의 Company만 삭제 가능
         */

        // 1. 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(companyDeleteCommand.callerId());

        // 2. 삭제 대상 Company 존재 여부 확인
        Company company = companyPersistenceService
                .getCompanyById(companyDeleteCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 3. 기존 Company의 소속 Hub 존재 여부 확인
        HubInfo hubInfo = hubPort.validateHub(company.getHubId());

        // 4. Company 삭제 권한 검증
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                hubInfo.hubId()
                        )
                );

        // 삭제 권한이 없는 경우
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        // 실제 DB 논리 삭제 시점부터 트랜잭션 시작
        return companyPersistenceService.deleteCompany(
                company.getId(),
                companyDeleteCommand.callerId()
        );
    }
}
