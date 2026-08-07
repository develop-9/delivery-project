package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.company.application.result.ProductGetResult;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.ProductQueryRepository;
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
public class ProductQueryService {

    private final ProductQueryRepository productQueryRepository;

    // 상품 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public ProductGetResult getProduct(ProductGetQuery productGetQuery) {

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        // 상품이 존재하는지 확인
        Product product = validateProduct(productGetQuery.productId());

        log.info("상품 조회 성공. productId={}",
                product.getId()
        );

        // 결과 반환
        return ProductGetResult.from(product);
    }

    // Validation Check - 상품 조회
    private Product validateProduct(UUID productId) {
        return productQueryRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
