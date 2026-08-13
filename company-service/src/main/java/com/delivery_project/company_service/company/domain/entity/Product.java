package com.delivery_project.company_service.company.domain.entity;

import com.delivery_project.company_service.global.common.BaseDeletableEntity;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Builder
    public Product(UUID companyId, String name, Integer price) {
        this.companyId = companyId;
        this.name = name;
        this.price = price;
    }

    private static final int MAX_NAME_LENGTH = 100;

    public static Product create(UUID companyId, String name, Integer price) {

        validateCompanyId(companyId);
        validateName(name);
        validatePrice(price);

        return Product.builder()
                .companyId(companyId)
                .name(name)
                .price(price)
                .build();
    }

    public void update(UUID companyId, String name, Integer price) {

        validateCompanyId(companyId);
        validateName(name);
        validatePrice(price);

        this.companyId = companyId;
        this.name = name;
        this.price = price;
    }

    // Validation Check - 올바른 업체 ID 기입 여부 판별
    private static void validateCompanyId(UUID companyId) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_COMPANY_ID);
        }
    }

    // Validation Check - 올바른 상품명 기입 여부 판멸
    private static void validateName(String name) {
        if (name == null
                || name.isBlank()
                || name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_NAME);
        }
    }

    // Validation Check - 올바른 금액 기입 여부 판별
    private static void validatePrice(Integer price) {
        if (price == null || price <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_PRICE);
        }
    }
}
