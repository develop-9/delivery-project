package com.delivery_project.company_service.company.application.pesistence_service;

import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductDeleteResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPersistenceService {

    private final ProductCommandRepository productCommandRepository;

    /*
     * TODO: 상품 식별자 추가 후 Unique 제약 적용
     * 현재 name, price만으로는 상품을 유일하게 식별할 수 없음
     *
     * 추후 상품 등록번호 등 유일 식별 필드가 추가되고
     * 해당 필드의 유일성이 비즈니스 규칙으로 확정되면
     * DB Unique 제약을 적용하여 중복 등록 및 데이터 무결성을 보장
     */
    @Transactional
    public ProductCreateResult saveProduct(UUID companyId, String name, Integer price) {
        // Product 생성
        Product product = Product.create(
                companyId,
                name,
                price
        );

        // 상품 저장
        Product savedProduct = productCommandRepository.save(product);

        // 허브별 상품의 재고 생성 요청
        // TODO: Order Service 연동 후 주석 제거
        // List<InventorySaveInfo> inventoryList = orderPort.saveInventory(productCreateResult.productId());

        log.info(
                "상품 생성 완료. productId={}, createdBy={}",
                savedProduct.getId(),
                savedProduct.getCreatedBy()
        );

        // 결과 반환
        return ProductCreateResult.from(savedProduct);
    }

    @Transactional
    public ProductUpdateResult updateProduct(Product product, UUID companyId, String name, Integer price) {
        // 상품 정보 변경
        product.update(
                companyId,
                name,
                price
        );

        log.info(
                "상품 수정 완료. productId={}, updatedBy={}",
                product.getId(),
                product.getUpdatedBy()
        );

        // 결과 반환
        return ProductUpdateResult.from(product);
    }

    @Transactional
    public ProductDeleteResult deleteProduct(Product product, UUID callerId) {
        // 상품의 재고 삭제 요청
        // TODO: Order Service 연동 후 주석 제거
        // orderPort.deleteInventory(product.getId());

        // 상품 제거
        product.delete(callerId);

        log.info(
                "상품 논리 삭제 완료. productId={}, deletedBy={}",
                product.getId(),
                product.getDeletedBy()
        );

        // 결과 반환
        return ProductDeleteResult.from(product);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(UUID productId) {
        return productCommandRepository.findById(productId);
    }
}
