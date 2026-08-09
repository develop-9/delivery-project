package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductDeleteResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.delivery_project.company_service.global.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductCommandRepository productCommandRepository;

    @Mock
    private CompanyQueryRepository companyQueryRepository;

    @Mock
    private UserPort userPort;

    @InjectMocks
    private ProductCommandService productCommandService;

    @Nested
    @DisplayName("상품 생성 비즈니스 로직 테스트")
    class CreateProduct {

        @Test
        @DisplayName("Master가 상품 생성에 성공한다.")
        void createProduct_success_whenMaster() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
                    Role.MASTER,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

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

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(productCommandRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("담당 Hub Manager가 상품 생성에 성공한다.")
        void createProduct_success_whenHubManager() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
                    Role.HUB_MANAGER,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

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

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(productCommandRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("담당 Company Manager가 상품 생성에 성공한다.")
        void createProduct_success_whenCompanyManager() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
                    Role.COMPANY_MANAGER,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

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

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(companyId);

            given(productCommandRepository.save(any(Product.class)))
                    .willReturn(savedProduct);

            // when
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .save(any(Product.class));
        }

        @Test
        @DisplayName("존재하지 않는 업체의 상품을 생성하면 COMPANY_NOT_FOUND 예외가 발생한다.")
        void createProduct_fail_whenCompanyNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
                    Role.MASTER,
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.createProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(productCommandRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("상품 생성 권한이 없으면 AUTH_FORBIDDEN 예외가 발생한다.")
        void createProduct_fail_whenForbidden() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
                    Role.HUB_MANAGER,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(UUID.randomUUID());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.createProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("상품 수정 비즈니스 로직 테스트")
    class UpdateProduct {

        @Test
        @DisplayName("Master가 상품 정보를 정상적으로 수정한다.")
        void updateProduct_success_whenMaster() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID newCompanyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    Role.MASTER,
                    productId,
                    newCompanyId,
                    "수정된 상품",
                    20000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    newCompanyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            Product product = new Product(
                    productId,
                    companyId,
                    "기존 상품",
                    10000
            );

            given(companyQueryRepository.findById(newCompanyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // when
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getCompanyId())
                    .isEqualTo(newCompanyId);

            assertThat(product.getName())
                    .isEqualTo("수정된 상품");

            assertThat(product.getPrice())
                    .isEqualTo(20000);

            then(companyQueryRepository)
                    .should()
                    .findById(newCompanyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .findById(productId);
        }

        @Test
        @DisplayName("담당 Hub Manager가 상품 정보를 정상적으로 수정한다.")
        void updateProduct_success_whenHubManager() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID newCompanyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    Role.HUB_MANAGER,
                    productId,
                    newCompanyId,
                    "수정된 상품",
                    20000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    newCompanyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            Product product = new Product(
                    productId,
                    companyId,
                    "기존 상품",
                    10000
            );

            given(companyQueryRepository.findById(newCompanyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // when
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getCompanyId())
                    .isEqualTo(newCompanyId);

            assertThat(product.getName())
                    .isEqualTo("수정된 상품");

            assertThat(product.getPrice())
                    .isEqualTo(20000);

            then(companyQueryRepository)
                    .should()
                    .findById(newCompanyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .findById(productId);
        }

        @Test
        @DisplayName("담당 Company Manager가 상품 정보를 정상적으로 수정한다.")
        void updateProduct_success_whenCompanyManager() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    Role.COMPANY_MANAGER,
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            Product product = new Product(
                    productId,
                    companyId,
                    "기존 상품",
                    10000
            );

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(companyId);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // when
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getCompanyId())
                    .isEqualTo(companyId);

            assertThat(product.getName())
                    .isEqualTo("수정된 상품");

            assertThat(product.getPrice())
                    .isEqualTo(20000);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 업체로 상품을 수정하면 COMPANY_NOT_FOUND 예외가 발생한다.")
        void updateProduct_fail_whenCompanyNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    Role.MASTER,
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(productCommandRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("상품 수정 권한이 없으면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateProduct_fail_whenForbidden() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    Role.HUB_MANAGER,
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            Company company = Company.builder()
                    .hubId(companyHubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 PRODUCT_NOT_FOUND 예외가 발생한다.")
        void updateProduct_fail_whenProductNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    Role.MASTER,
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(productCommandRepository)
                    .should()
                    .findById(productId);
        }
    }

    @Nested
    @DisplayName("상품 삭제 비즈니스 로직 테스트")
    class DeleteProduct {

        @Test
        @DisplayName("Master가 상품을 정상적으로 논리 삭제한다.")
        void deleteProduct_success_whenMaster() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            Role.MASTER,
                            productId
                    );

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            // when
            ProductDeleteResult result =
                    productCommandService.deleteProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getDeletedAt())
                    .isNotNull();

            assertThat(product.getDeletedBy())
                    .isEqualTo(callerId);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }

        @Test
        @DisplayName("담당 Hub Manager가 상품을 정상적으로 논리 삭제한다.")
        void deleteProduct_success_whenHubManager() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            Role.HUB_MANAGER,
                            productId
                    );

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            // when
            ProductDeleteResult result =
                    productCommandService.deleteProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            assertThat(product.getDeletedAt())
                    .isNotNull();

            assertThat(product.getDeletedBy())
                    .isEqualTo(callerId);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 PRODUCT_NOT_FOUND 예외가 발생한다.")
        void deleteProduct_fail_whenProductNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            Role.HUB_MANAGER,
                            productId
                    );

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(companyQueryRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("상품을 관리하는 업체가 존재하지 않으면 COMPANY_NOT_FOUND 예외가 발생한다.")
        void deleteProduct_fail_whenCompanyNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            Role.MASTER,
                            productId
                    );

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }

        @Test
        @DisplayName("담당하지 않는 Hub의 Hub Manager가 상품을 삭제하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void deleteProduct_fail_whenHubManagerOfDifferentHub() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            Role.HUB_MANAGER,
                            productId
                    );

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(companyHubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }

        @Test
        @DisplayName("Company Manager가 상품을 삭제하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void deleteProduct_fail_whenCompanyManager() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            Role.COMPANY_MANAGER,
                            productId
                    );

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(productCommandRepository.findById(productId))
                    .willReturn(Optional.of(product));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(productCommandRepository)
                    .should()
                    .findById(productId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }
    }
}
