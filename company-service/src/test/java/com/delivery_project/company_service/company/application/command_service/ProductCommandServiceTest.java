package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
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
}
