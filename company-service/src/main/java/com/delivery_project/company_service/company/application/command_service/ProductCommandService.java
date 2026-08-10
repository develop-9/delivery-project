package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.pesistence_service.CompanyPersistenceService;
import com.delivery_project.company_service.company.application.pesistence_service.ProductPersistenceService;
import com.delivery_project.company_service.company.application.port.OrderPort;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
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
    private final OrderPort orderPort;

    // [외부] 상품 생성 비즈니스 로직
    public ProductCreateResult createProduct(ProductCreateCommand productCreateCommand) {

        log.info(
                "상품 생성 요청. callerId={}",
                productCreateCommand.callerId()
        );

        /*
         * 상품 생성 검증
         * 1. 존재 여부 검증
         *  - 실제 존재하는 Company인지 확인
         *  - 이때, 존재 여부가 권한이 없는 사용자에게 리소스가 노출되지 않도록 권한 오류와 동일하게 처리
         *
         * 2. 권한 검증
         *  - Master, 담당 Hub Manager, 담당 Company Manager만 가능
         */

        // 업체를 조회
        Company company = companyPersistenceService
                .getCompanyById(productCreateCommand.companyId())
                /*
                 * 조회된 업체가 없는 경우
                 * Company의 존재 여부가 권한이 없는 사용자에게 노출되지 않도록
                 * 존재하지 않는 경우와 접근 권한이 없는 경우를 동일한 오류로 처리
                 */
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productCreateCommand.callerId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능하도록 검증
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                company.getHubId()
                        )
                )
                        || (
                        callerInfo.role() == Role.COMPANY_MANAGER
                                && Objects.equals(
                                callerInfo.companyId(),
                                company.getId()
                        )
                );

        // 권한이 없을 경우 오류 반환
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        // 실제 DB 저장 시점부터 트랜잭션 시작
        ProductCreateResult productCreateResult
                = productPersistenceService.saveProduct(
                        productCreateCommand.companyId(),
                        productCreateCommand.name(),
                        productCreateCommand.price()
        );

        // Inventory에 Hub별로 Product와 수량(0) 저장
        // 추후 연동시 주석 제거
        // List<InventorySaveInfo> inventoryList = orderPort.saveInventory(productCreateResult.productId());

        // 결과 반환
        return productCreateResult;
    }

    // [외부] 상품 수정 비즈니스 로직
    public ProductUpdateResult updateProduct(ProductUpdateCommand productUpdateCommand) {

        log.info(
                "상품 수정 요청. callerId={}",
                productUpdateCommand.callerId()
        );

        /*
         * 상품 수정 검증
         * 1. 존재 여부 검증
         *  - 실제 존재하는 Company인지 확인
         *  - 이때, 존재 여부가 권한이 없는 사용자에게 리소스가 노출되지 않도록 권한 오류와 동일하게 처리
         *
         * 2. 권한 검증
         *  - Master, 담당 Hub Manager, 담당 Company Manager만 가능
         */

        /*
         * 업체 존재 여부를 확인
         * 존재하지 않는 경우에도 Company의 존재 여부가
         * 권한이 없는 사용자에게 노출되지 않도록 AUTH_FORBIDDEN으로 처리
         */
        Company company = companyPersistenceService
                .getCompanyById(productUpdateCommand.companyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productUpdateCommand.callerId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능하도록 검증
        boolean hasPermission =
                callerInfo.role() == Role.MASTER
                        || (
                        callerInfo.role() == Role.HUB_MANAGER
                                && Objects.equals(
                                callerInfo.hubId(),
                                company.getHubId()
                        )
                )
                        || (
                        callerInfo.role() == Role.COMPANY_MANAGER
                                && Objects.equals(
                                callerInfo.companyId(),
                                company.getId()
                        )
                );

        // 권한이 없을 경우 오류 반환
        if (!hasPermission) {
            throw new BusinessException(
                    ErrorCode.AUTH_FORBIDDEN
            );
        }

        // 상품을 조회
        Product product = productPersistenceService
                .getProductById(productUpdateCommand.productId())
                // 조회된 상품이 없는 경우
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 실제 DB 수정 시점부터 트랜잭션 시작
        return productPersistenceService.updateProduct(
                product,
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
         * 상품 수정 검증
         * 1. 존재 여부 검증
         *  - 실제 존재하는 Product, Company인지 확인
         *  - 이때, 존재 여부가 권한이 없는 사용자에게 리소스가 노출되지 않도록 권한 오류와 동일하게 처리
         *
         * 2. 권한 검증
         *  - Master, 담당 Hub Manager, 담당 Company Manager만 가능
         */

        /*
         * 상품 존재 여부를 확인
         * 존재하지 않는 경우에도 Product의 존재 여부가
         * 권한이 없는 사용자에게 노출되지 않도록 AUTH_FORBIDDEN으로 처리
         */
        Product product = productPersistenceService
                .getProductById(productDeleteCommand.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        /*
         * 업체 존재 여부를 확인
         * 존재하지 않는 경우에도 Company의 존재 여부가
         * 권한이 없는 사용자에게 노출되지 않도록 AUTH_FORBIDDEN으로 처리
         */
        Company company = companyPersistenceService
                .getCompanyById(product.getCompanyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));

        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productDeleteCommand.callerId());

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

        // 실제 DB 삭제 시점부터 트랜잭션 시작
        return productPersistenceService.deleteProduct(
                product,
                productDeleteCommand.callerId()
        );
    }
}
