package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
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

@ExtendWith(MockitoExtension.class)
class ProductCommandRepositoryImplTest {

    @Mock
    private SpringDataProductRepository springDataProductRepository;

    @InjectMocks
    private ProductCommandRepositoryImpl productCommandRepository;

    @Nested
    @DisplayName("상품 저장")
    class Save {

        @Test
        @DisplayName("상품을 저장하고 저장된 상품을 반환한다")
        void save_success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(springDataProductRepository.save(product))
                    .willReturn(product);

            // when
            Product result = productCommandRepository.save(product);

            // then
            assertThat(result)
                    .isSameAs(product);

            then(springDataProductRepository)
                    .should()
                    .save(product);
        }

        @Test
        @DisplayName("상품 저장 중 예외가 발생하면 예외를 그대로 전달한다")
        void save_fail() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            RuntimeException exception =
                    new RuntimeException("상품 저장 실패");

            given(springDataProductRepository.save(product))
                    .willThrow(exception);

            // when & then
            assertThatThrownBy(
                    () -> productCommandRepository.save(product)
            )
                    .isSameAs(exception);

            then(springDataProductRepository)
                    .should()
                    .save(product);
        }
    }

    @Nested
    @DisplayName("상품 단건 조회 테스트")
    class FindById {

        @Test
        @DisplayName("상품 ID로 상품을 정상적으로 조회한다")
        void findById_success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(springDataProductRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // when
            Optional<Product> result =
                    productCommandRepository.findById(productId);

            // then
            assertThat(result)
                    .isPresent();

            assertThat(result.get())
                    .isSameAs(product);

            then(springDataProductRepository)
                    .should()
                    .findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 상품 ID로 조회하면 빈 Optional을 반환한다")
        void findById_notFound() {
            // given
            UUID productId = UUID.randomUUID();

            given(springDataProductRepository.findById(productId))
                    .willReturn(Optional.empty());

            // when
            Optional<Product> result =
                    productCommandRepository.findById(productId);

            // then
            assertThat(result)
                    .isEmpty();

            then(springDataProductRepository)
                    .should()
                    .findById(productId);
        }
    }
}
