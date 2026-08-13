package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.persistence_service.CompanyPersistenceService;
import com.delivery_project.company_service.company.application.persistence_service.ProductPersistenceService;
import com.delivery_project.company_service.company.application.port.HubPort;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.port.dto.HubInfo;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductDeleteResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.Product;
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
public class ProductCommandService {

    private final CompanyPersistenceService companyPersistenceService;
    private final ProductPersistenceService productPersistenceService;
    private final UserPort userPort;
    private final HubPort hubPort;

    // [외부] 상품 생성 비즈니스 로직
    public ProductCreateResult createProduct(ProductCreateCommand productCreateCommand) {

        log.info(
                "상품 생성 요청. callerId={}",
                productCreateCommand.callerId()
        );

        /*
         * 상품 생성 검증 순서
         *
         * 1. 요청자 정보 조회
         *
         * 2. 생성 대상 Product의 소속 Company 존재 여부 확인
         *  - Product는 실제 존재하는 Company에 소속되어 있어야 함
         *  - Company가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Company 존재 여부가 외부에 노출되지 않도록 함
         *
         * 3. 기존 Company의 소속 Hub 존재 여부 확인
         *  - Product를 생성하려면 Company가 현재 접근 가능한 Hub에
         *    소속되어 있어야 함
         *  - Hub가 삭제된 경우 해당 Company에 대한 Product 생성 불가
         *  - Hub가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Hub 존재 여부가 외부에 노출되지 않도록 함
         *
         * 4. Product 생성 권한 검증
         *  - Master: 모든 Product 생성 가능
         *  - Hub Manager: 담당 Hub의 Company에 소속된 Product만 생성 가능
         *  - Company Manager: 담당 Company에 소속된 Product만 생성 가능
         */

        // 1. 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productCreateCommand.callerId());

        // 2. 생성 대상 Product의 소속 Company 존재 여부 확인
        Company company = companyPersistenceService
                .getCompanyById(productCreateCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 3. 기존 Company의 소속 Hub 존재 여부 확인
        HubInfo hubInfo = hubPort.validateHub(company.getHubId());

        // 4. Product 생성 권한 검증
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

        // 생성 권한이 없는 경우
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        // 실제 DB 저장 시점부터 트랜잭션 시작
        return productPersistenceService.saveProduct(
                        productCreateCommand.companyId(),
                        productCreateCommand.name(),
                        productCreateCommand.price()
        );
    }

    // [외부] 상품 수정 비즈니스 로직
    public ProductUpdateResult updateProduct(ProductUpdateCommand productUpdateCommand) {

        log.info(
                "상품 수정 요청. callerId={}",
                productUpdateCommand.callerId()
        );

        /*
         * 상품 수정 검증 순서
         *
         * 1. 요청자 정보 조회
         *
         * 2. 수정 대상 Product 존재 여부 확인
         *  - 실제 존재하는 Product인지 확인
         *  - Product가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Product 존재 여부가 외부에 노출되지 않도록 함
         *
         * 3. Product가 소속된 Company 존재 여부 확인
         *  - Product는 실제 존재하는 Company에 소속되어 있어야 함
         *  - Company가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Company 존재 여부가 외부에 노출되지 않도록 함
         *
         * 4. 기존 Company의 소속 Hub 존재 여부 확인
         *  - Product를 수정하려면 Company가 현재 접근 가능한 Hub에
         *    소속되어 있어야 함
         *  - Hub가 삭제된 경우 해당 Company의 Product 수정 불가
         *  - Hub가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Hub 존재 여부가 외부에 노출되지 않도록 함
         *
         * 5. Product 수정 권한 검증
         *  - Master: 모든 Product 수정 가능
         *  - Hub Manager: 담당 Hub의 Company에 소속된 Product만 수정 가능
         *  - Company Manager: 담당 Company에 소속된 Product만 수정 가능
         */

        // 1. 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productUpdateCommand.callerId());

        // 2. 수정 대상 Product 존재 여부 확인
        Product product = productPersistenceService
                .getProductById(productUpdateCommand.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 3. Product가 소속된 Company 존재 여부 확인
        Company company = companyPersistenceService
                .getCompanyById(product.getCompanyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 4. 기존 Company의 소속 Hub 존재 여부 확인
        HubInfo hubInfo = hubPort.validateHub(company.getHubId());

        // 5. Product 수정 권한 검증
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

        // 실제 DB 수정 시점부터 트랜잭션 시작
        return productPersistenceService.updateProduct(
                product.getId(),
                productUpdateCommand.companyId(),
                productUpdateCommand.name(),
                productUpdateCommand.price()
        );
    }

    // [외부] 상품 삭제 비즈니스 로직
    public ProductDeleteResult deleteProduct(ProductDeleteCommand productDeleteCommand) {

        log.info(
                "상품 삭제 요청. callerId={}",
                productDeleteCommand.callerId()
        );

        /*
         * 상품 삭제 검증 순서
         *
         * 1. 요청자 정보 조회
         *
         * 2. 삭제 대상 Product 존재 여부 확인
         *  - 실제 존재하는 Product인지 확인
         *  - Product가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Product 존재 여부가 외부에 노출되지 않도록 함
         *
         * 3. Product가 소속된 Company 존재 여부 확인
         *  - Product는 실제 존재하는 Company에 소속되어 있어야 함
         *  - Company가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Company 존재 여부가 외부에 노출되지 않도록 함
         *
         * 4. 기존 Company의 소속 Hub 존재 여부 확인
         *  - Product를 삭제하려면 Company가 현재 접근 가능한 Hub에
         *    소속되어 있어야 함
         *  - Hub가 삭제된 경우 해당 Company의 Product 삭제 불가
         *  - Hub가 존재하지 않는 경우 AUTH_FORBIDDEN으로 처리하여
         *    Hub 존재 여부가 외부에 노출되지 않도록 함
         *
         * 5. Product 삭제 권한 검증
         *  - Master: 모든 Product 삭제 가능
         *  - Hub Manager: 담당 Hub의 Company에 소속된 Product만 삭제 가능
         *  - Company Manager: 담당 Company에 소속된 Product만 삭제 가능
         */

        // 1. 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productDeleteCommand.callerId());

        // 2. 삭제 대상 Product 존재 여부 확인
        Product product = productPersistenceService
                .getProductById(productDeleteCommand.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 3. Product가 소속된 Company 존재 여부 확인
        Company company = companyPersistenceService
                .getCompanyById(product.getCompanyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 4. 기존 Company의 소속 Hub 존재 여부 확인
        HubInfo hubInfo = hubPort.validateHub(company.getHubId());

        // 5. Product 삭제 권한 검증
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

        // 실제 DB 삭제 시점부터 트랜잭션 시작
        return productPersistenceService.deleteProduct(
                product.getId(),
                productDeleteCommand.callerId()
        );
    }
}
