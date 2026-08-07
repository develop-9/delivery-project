package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.company.application.result.ProductGetResult;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.infrastructure.persistence.ProductQueryRepositoryImpl;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductQueryRepositoryImpl productRepository;

    @InjectMocks
    private ProductQueryService productQueryService;

    @Nested
    @DisplayName("상품 단건 조회 비즈니스 로직 검증")
    class GetProduct {

        @Test
        @DisplayName("상품 ID로 상품을 조회할 수 있다.")
        void getProduct_success() {
            // Given
            UUID productId = UUID.randomUUID();

            Product product = mock(Product.class);
            given(product.getId()).willReturn(productId);

            ProductGetQuery productGetQuery =
                    new ProductGetQuery(productId);

            given(productRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // When
            ProductGetResult result =
                    productQueryService.getProduct(productGetQuery);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.productId()).isEqualTo(productId);

            then(productRepository)
                    .should(times(1))
                    .findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다.")
        void getProduct_fail_productNotFound() {
            // Given
            UUID productId = UUID.randomUUID();

            ProductGetQuery productGetQuery =
                    new ProductGetQuery(productId);

            given(productRepository.findById(productId))
                    .willReturn(Optional.empty());

            // When
            // Then
            assertThatThrownBy(
                    () -> productQueryService.getProduct(productGetQuery)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.PRODUCT_NOT_FOUND
                    );

            then(productRepository)
                    .should(times(1))
                    .findById(productId);
        }
    }
}
