package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.port.OrderPort;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.port.dto.InventorySaveInfo;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductDeleteResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.delivery_project.company_service.global.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final CompanyQueryRepository companyQueryRepository;
    private final ProductCommandRepository productCommandRepository;
    private final UserPort userPort;
    private final OrderPort orderPort;

    // [외부] 상품 생성 비즈니스 로직
    @Transactional
    public ProductCreateResult createProduct(ProductCreateCommand productCreateCommand) {

        log.info(
                "상품 생성 요청. callerId={}",
                productCreateCommand.callerId()
        );

        // 업체가 존재하는지 확인
        Company company = validateCompany(productCreateCommand.companyId());

        // 상품 생성 권한 검증
        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productCreateCommand.callerId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능
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

        // Product 생성
        Product product = Product.create(
                productCreateCommand.companyId(),
                productCreateCommand.name(),
                productCreateCommand.price()
        );

        // 상품 저장
        Product savedProduct = productCommandRepository.save(product);

        // Inventory에 Hub별로 Product와 수량(0) 저장
        // 추후 연동시 주석 제거
        // List<InventorySaveInfo> inventoryList = orderPort.saveInventory(savedProduct.getId());

        log.info(
                "상품 생성 완료. productId={}, createdBy={}",
                savedProduct.getId(),
                savedProduct.getCreatedBy()
                // inventoryList.size()
        );

        // 결과 반환
        return ProductCreateResult.from(savedProduct);
    }

    // [외부] 상품 수정 비즈니스 로직
    @Transactional
    public ProductUpdateResult updateProduct(ProductUpdateCommand productUpdateCommand) {

        log.info(
                "상품 수정 요청. callerId={}",
                productUpdateCommand.callerId()
        );

        // 업체가 존재하는지 확인
        Company company = validateCompany(productUpdateCommand.companyId());

        // 상품 수정 권한 검증
        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productUpdateCommand.callerId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능
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

        // 상품이 존재하는지 확인
        Product product = validateProduct(productUpdateCommand.productId());

        // 상품 정보 변경
        product.update(
                productUpdateCommand.companyId(),
                productUpdateCommand.name(),
                productUpdateCommand.price()
        );

        log.info(
                "상품 수정 완료. productId={}, updatedBy={}",
                product.getId(),
                product.getUpdatedBy()
        );

        // 결과 반환
        return ProductUpdateResult.from(product);
    }

    // [외부] 상품 삭제 비즈니스 로직
    @Transactional
    public ProductDeleteResult deleteProduct(ProductDeleteCommand productDeleteCommand) {

        log.info(
                "상품 삭제 요청. callerId={}",
                productDeleteCommand.callerId()
        );

        // 상품이 존재하는지 확인
        Product product = validateProduct(productDeleteCommand.productId());

        // 상품 삭제 권한 검증
        // 요청자 정보 조회
        CallerInfo callerInfo =
                userPort.getCaller(productDeleteCommand.callerId());

        // 상품을 관리하는 업체 조회
        Company company = validateCompany(product.getCompanyId());

        // Master, 담당 Hub Manager, 담당 Company Manager만 가능
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

        // Inventory에 Hub별로 저장된 Product 제거
        // 추후 연동시 주석 제거
        // orderPort.deleteInventory(product.getId());

        // 상품 제거
        product.delete(productDeleteCommand.callerId());

        log.info(
                "상품 논리 삭제 완료. productId={}, deletedBy={}",
                product.getId(),
                product.getDeletedBy()
        );

        // 결과 반환
        return ProductDeleteResult.from(product);
    }


    // Validation Check - 업체 존재 여부 판단
    private Company validateCompany(UUID companyId) {
        return companyQueryRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }

    // Validation Check - 상품 조회
    private Product validateProduct(UUID productId) {
        return productCommandRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
