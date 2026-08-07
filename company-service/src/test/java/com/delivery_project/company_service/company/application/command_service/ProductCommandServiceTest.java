package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductCommandRepository productCommandRepository;

    @Mock
    private CompanyQueryRepository companyQueryRepository;

    @InjectMocks
    private ProductCommandService productCommandService;

    @Nested
    @DisplayName("상품 생성 비즈니스 로직 테스트")
    class CreateProduct {

        @Test
        @DisplayName("업체가 존재하면 상품을 생성한다")
        void createProduct_success() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    companyId,
                    "테스트 상품",
                    10000
            );

            Product product = Product.create(
                    companyId,
                    "테스트 상품",
                    10000
            );

            Product savedProduct = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(companyQueryRepository.existsById(companyId))
                    .willReturn(true);

            given(productCommandRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.productId()).isEqualTo(productId);

            then(companyQueryRepository)
                    .should()
                    .existsById(companyId);

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("존재하지 않는 업체의 상품을 생성하면 COMPANY_NOT_FOUND 예외가 발생한다")
        void createProduct_companyNotFound() {
            // given
            UUID companyId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(companyQueryRepository.existsById(companyId))
                    .willReturn(false);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.createProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(companyQueryRepository)
                    .should()
                    .existsById(companyId);

            then(productCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("상품 수정 비즈니스 로직 테스트")
    class UpdateProduct {

        @Test
        @DisplayName("상품 정보를 정상적으로 수정한다")
        void updateProduct_success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID newCompanyId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    productId,
                    newCompanyId,
                    "수정된 상품",
                    20000
            );

            Product product = new Product(
                    productId,
                    companyId,
                    "기존 상품",
                    10000
            );

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(companyQueryRepository.existsById(newCompanyId))
                    .willReturn(true);

            // when
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getCompanyId())
                    .isEqualTo(newCompanyId);
            assertThat(product.getName())
                    .isEqualTo("수정된 상품");
            assertThat(product.getPrice())
                    .isEqualTo(20000);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(companyQueryRepository)
                    .should()
                    .existsById(newCompanyId);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        void updateProduct_productNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(companyQueryRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 업체로 상품을 수정하면 COMPANY_NOT_FOUND 예외가 발생한다")
        void updateProduct_companyNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            Product product = new Product(
                    productId,
                    companyId,
                    "기존 상품",
                    10000
            );

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(companyQueryRepository.existsById(companyId))
                    .willReturn(false);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(companyQueryRepository)
                    .should()
                    .existsById(companyId);

            assertThat(product.getCompanyId())
                    .isEqualTo(companyId);
            assertThat(product.getName())
                    .isEqualTo("기존 상품");
            assertThat(product.getPrice())
                    .isEqualTo(10000);
        }
    }
}
