package com.delivery_project.company_service.company.application.persistence_service;

import com.delivery_project.company_service.company.application.result.*;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductPersistenceServiceTest {

    @Mock
    private ProductCommandRepository productCommandRepository;

    @InjectMocks
    private ProductPersistenceService productPersistenceService;

    @Nested
    @DisplayName("상품 생성 트랜잭션 비즈니스 로직 테스트")
    class SaveProductPersistence {

        @Test
        @DisplayName("상품을 정상적으로 저장한다.")
        void saveProduct_success() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            String name = "테스트 상품";
            Integer price = 10000;

            Product savedProduct = Product.create(
                    companyId,
                    name,
                    price
            );

            ReflectionTestUtils.setField(
                    savedProduct,
                    "id",
                    productId
            );

            given(productCommandRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            ProductCreateResult result =
                    productPersistenceService.saveProduct(
                            companyId,
                            name,
                            price
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("전달받은 업체 ID로 상품을 생성한다.")
        void saveProduct_success_withCompanyId() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            String name = "테스트 상품";
            Integer price = 10000;

            Product savedProduct = Product.create(
                    companyId,
                    name,
                    price
            );

            ReflectionTestUtils.setField(
                    savedProduct,
                    "id",
                    productId
            );

            given(productCommandRepository.save(any(Product.class)))
                    .willAnswer(invocation -> {
                        Product product = invocation.getArgument(0);

                        assertThat(product.getCompanyId())
                                .isEqualTo(companyId);

                        assertThat(product.getName())
                                .isEqualTo(name);

                        assertThat(product.getPrice())
                                .isEqualTo(price);

                        return savedProduct;
                    });

            // when
            ProductCreateResult result =
                    productPersistenceService.saveProduct(
                            companyId,
                            name,
                            price
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("저장된 상품의 ID를 반환한다.")
        void saveProduct_success_returnsSavedProductId() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Product savedProduct = Product.create(
                    companyId,
                    "테스트 상품",
                    10000
            );

            ReflectionTestUtils.setField(
                    savedProduct,
                    "id",
                    productId
            );

            given(productCommandRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            ProductCreateResult result =
                    productPersistenceService.saveProduct(
                            companyId,
                            "테스트 상품",
                            10000
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(savedProduct.getId());

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("상품 저장에 실패하면 예외가 전파된다.")
        void saveProduct_fail_whenRepositoryThrowsException() {
            // given
            UUID companyId = UUID.randomUUID();

            given(productCommandRepository.save(any(Product.class)))
                    .willThrow(new RuntimeException("상품 저장 실패"));

            // when & then
            assertThatThrownBy(() ->
                    productPersistenceService.saveProduct(
                            companyId,
                            "테스트 상품",
                            10000
                    )
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("상품 저장 실패");

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("상품 수정 트랜잭션 비즈니스 로직 테스트")
    class UpdateProductPersistence {

        @Test
        @DisplayName("상품 정보를 정상적으로 수정한다.")
        void updateProduct_success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID newCompanyId = UUID.randomUUID();

            Product product = new Product(
                    productId,
                    companyId,
                    "기존 상품",
                    10000
            );

            String newName = "수정된 상품";
            Integer newPrice = 20000;

            // when
            ProductUpdateResult result =
                    productPersistenceService.updateProduct(
                            product,
                            newCompanyId,
                            newName,
                            newPrice
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getCompanyId())
                    .isEqualTo(newCompanyId);

            assertThat(product.getName())
                    .isEqualTo(newName);

            assertThat(product.getPrice())
                    .isEqualTo(newPrice);
        }
    }

    @Nested
    @DisplayName("상품 삭제 트랜잭션 비즈니스 로직 테스트")
    class DeleteProductPersistence {

        @Test
        @DisplayName("상품을 정상적으로 논리 삭제한다.")
        void deleteProduct_success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID callerId = UUID.randomUUID();

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            // when
            ProductDeleteResult result =
                    productPersistenceService.deleteProduct(
                            product,
                            callerId
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getDeletedAt())
                    .isNotNull();

            assertThat(product.getDeletedBy())
                    .isEqualTo(callerId);
        }
    }
}
