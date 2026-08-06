package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
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
public class ProductCommandService {

    private final CompanyQueryRepository companyQueryRepository;
    private final ProductCommandRepository productCommandRepository;

    @Transactional
    public ProductCreateResult createProduct(ProductCreateCommand productCreateCommand) {

        // Company 존재 여부 검증
        validateCompany(productCreateCommand.companyId());

        // Product 생성
        Product product = Product.create(
                productCreateCommand.companyId(),
                productCreateCommand.name(),
                productCreateCommand.price()
        );

        // Product 저장
        Product savedProduct = productCommandRepository.save(product);

        // Inventory에 Hub별로 Product와 수량(0) 저장
        /*
         * TODO:
         *  Inventory 호출하여 각 Hub별로 Product 생성
         *  이때, 수량은 0으로 설정
         */

        log.info(
                "상품 생성 완료. productId={}, createdBy={}",
                savedProduct.getId(),
                savedProduct.getCreatedBy()
        );

        // 결과 반환
        return ProductCreateResult.from(savedProduct);
    }

    // Validation Check - 업체 존재 여부 판단
    private void validateCompany(UUID companyId) {
        if (!companyQueryRepository.existsById(companyId)) {
            throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
        }
    }
}
