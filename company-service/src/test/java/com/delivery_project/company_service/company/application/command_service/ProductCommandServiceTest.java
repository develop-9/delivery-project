package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.persistence_service.CompanyPersistenceService;
import com.delivery_project.company_service.company.application.persistence_service.ProductPersistenceService;
import com.delivery_project.company_service.company.application.port.HubPort;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.port.dto.HubInfo;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductDeleteResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.entity.Product;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private CompanyPersistenceService companyPersistenceService;

    @Mock
    private ProductPersistenceService productPersistenceService;

    @Mock
    private UserPort userPort;

    @Mock
    private HubPort hubPort;

    @InjectMocks
    private ProductCommandService productCommandService;

    @Nested
    @DisplayName("상품 생성 외부 비즈니스 로직 테스트")
    class CreateProductCommand {

        @Test
        @DisplayName("Master가 상품 생성에 성공한다.")
        void createProduct_success_whenMaster() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
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

            ProductCreateResult createResult =
                    new ProductCreateResult(productId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.saveProduct(
                    companyId,
                    "테스트 상품",
                    10000
            ))
                    .willReturn(createResult);

            // When
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .saveProduct(
                            companyId,
                            "테스트 상품",
                            10000
                    );
        }

        @Test
        @DisplayName("담당 Hub Manager가 상품 생성에 성공한다.")
        void createProduct_success_whenHubManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
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

            ProductCreateResult createResult =
                    new ProductCreateResult(productId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.saveProduct(
                    companyId,
                    "테스트 상품",
                    10000
            ))
                    .willReturn(createResult);

            // When
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .saveProduct(
                            companyId,
                            "테스트 상품",
                            10000
                    );
        }

        @Test
        @DisplayName("담당 Company Manager가 상품 생성에 성공한다.")
        void createProduct_success_whenCompanyManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
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

            ProductCreateResult createResult =
                    new ProductCreateResult(productId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(companyId);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.saveProduct(
                    companyId,
                    "테스트 상품",
                    10000
            ))
                    .willReturn(createResult);

            // When
            ProductCreateResult result =
                    productCommandService.createProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .saveProduct(
                            companyId,
                            "테스트 상품",
                            10000
                    );
        }

        @Test
        @DisplayName("존재하지 않는 업체의 상품을 생성하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void createProduct_fail_whenCompanyNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.empty());

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.createProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("상품 생성 권한이 없으면 AUTH_FORBIDDEN 예외가 발생한다.")
        void createProduct_fail_whenForbidden() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(UUID.randomUUID());

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.createProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 Hub의 업체에 상품을 생성하면 HUB_NOT_FOUND 예외가 발생한다.")
        void createProduct_fail_whenHubNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductCreateCommand command = new ProductCreateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willThrow(
                            new BusinessException(ErrorCode.HUB_NOT_FOUND)
                    );

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.createProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.HUB_NOT_FOUND);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("상품 수정 외부 비즈니스 로직 테스트")
    class UpdateProductCommand {

        @Test
        @DisplayName("Master가 상품 정보를 정상적으로 수정한다.")
        void updateProduct_success_whenMaster() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID newCompanyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            ProductUpdateResult updateResult =
                    new ProductUpdateResult(productId);

            given(companyPersistenceService.getCompanyById(newCompanyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(productPersistenceService.updateProduct(
                    product.getId(),
                    newCompanyId,
                    "수정된 상품",
                    20000
            ))
                    .willReturn(updateResult);

            // When
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(newCompanyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(productPersistenceService)
                    .should()
                    .updateProduct(
                            product.getId(),
                            newCompanyId,
                            "수정된 상품",
                            20000
                    );
        }

        @Test
        @DisplayName("담당 Hub Manager가 상품 정보를 정상적으로 수정한다.")
        void updateProduct_success_whenHubManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID newCompanyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            ProductUpdateResult updateResult =
                    new ProductUpdateResult(productId);

            given(companyPersistenceService.getCompanyById(newCompanyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(productPersistenceService.updateProduct(
                    product.getId(),
                    newCompanyId,
                    "수정된 상품",
                    20000
            ))
                    .willReturn(updateResult);

            // When
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(newCompanyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(productPersistenceService)
                    .should()
                    .updateProduct(
                            product.getId(),
                            newCompanyId,
                            "수정된 상품",
                            20000
                    );
        }

        @Test
        @DisplayName("담당 Company Manager가 상품 정보를 정상적으로 수정한다.")
        void updateProduct_success_whenCompanyManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            ProductUpdateResult updateResult =
                    new ProductUpdateResult(productId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(companyId);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(productPersistenceService.updateProduct(
                    product.getId(),
                    companyId,
                    "수정된 상품",
                    20000
            ))
                    .willReturn(updateResult);

            // When
            ProductUpdateResult result =
                    productCommandService.updateProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(productPersistenceService)
                    .should()
                    .updateProduct(
                            product.getId(),
                            companyId,
                            "수정된 상품",
                            20000
                    );
        }

        @Test
        @DisplayName("존재하지 않는 업체로 상품을 수정하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateProduct_fail_whenCompanyNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
                    productId,
                    companyId,
                    "수정된 상품",
                    20000
            );

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.empty());

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("상품 수정 권한이 없으면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateProduct_fail_whenForbidden() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 PRODUCT_NOT_FOUND 예외가 발생한다.")
        void updateProduct_fail_whenProductNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.empty());

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(productPersistenceService)
                    .should(never())
                    .updateProduct(
                            any(UUID.class),
                            any(UUID.class),
                            anyString(),
                            anyInt()
                    );
        }

        @Test
        @DisplayName("존재하지 않는 Hub의 업체로 상품을 수정하면 HUB_NOT_FOUND 예외가 발생한다.")
        void updateProduct_fail_whenHubNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willThrow(
                            new BusinessException(ErrorCode.HUB_NOT_FOUND)
                    );

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.HUB_NOT_FOUND);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should(never())
                    .getProductById(any(UUID.class));

            then(productPersistenceService)
                    .should(never())
                    .updateProduct(
                            any(UUID.class),
                            any(UUID.class),
                            anyString(),
                            anyInt()
                    );
        }

        @Test
        @DisplayName("담당하지 않는 Hub의 업체를 Hub Manager가 수정하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateProduct_fail_whenHubManagerOfDifferentHub() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("담당하지 않는 업체를 Company Manager가 수정하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateProduct_fail_whenCompanyManagerOfDifferentCompany() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID callerCompanyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductUpdateCommand command = new ProductUpdateCommand(
                    callerId,
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(callerCompanyId);

            // When & Then
            BusinessException exception = catchThrowableOfType(
                    () -> productCommandService.updateProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("상품 삭제 외부 비즈니스 로직 테스트")
    class DeleteProductCommand {

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
            HubInfo hubInfo = mock(HubInfo.class);

            ProductDeleteResult deleteResult =
                    mock(ProductDeleteResult.class);

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willReturn(hubInfo);

            given(productPersistenceService.deleteProduct(
                    product.getId(),
                    callerId
            ))
                    .willReturn(deleteResult);

            given(deleteResult.productId())
                    .willReturn(productId);

            // when
            ProductDeleteResult result =
                    productCommandService.deleteProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .deleteProduct(
                            product.getId(),
                            callerId
                    );
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
            HubInfo hubInfo = mock(HubInfo.class);

            ProductDeleteResult deleteResult =
                    mock(ProductDeleteResult.class);

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(hubPort.getHub(hubId))
                    .willReturn(hubInfo);

            given(productPersistenceService.deleteProduct(
                    product.getId(),
                    callerId
            ))
                    .willReturn(deleteResult);

            given(deleteResult.productId())
                    .willReturn(productId);

            // when
            ProductDeleteResult result =
                    productCommandService.deleteProduct(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(productId);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should()
                    .deleteProduct(
                            product.getId(),
                            callerId
                    );
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void deleteProduct_fail_whenProductNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            productId
                    );

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .shouldHaveNoInteractions();

            then(userPort)
                    .shouldHaveNoInteractions();

            then(hubPort)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("상품을 관리하는 업체가 존재하지 않으면 AUTH_FORBIDDEN 예외가 발생한다.")
        void deleteProduct_fail_whenCompanyNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
                            productId
                    );

            Product product = new Product(
                    productId,
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(hubPort)
                    .shouldHaveNoInteractions();
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

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .should(never())
                    .deleteProduct(
                            any(UUID.class),
                            any(UUID.class)
                    );
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

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(productPersistenceService)
                    .should(never())
                    .deleteProduct(
                            any(UUID.class),
                            any(UUID.class)
                    );
        }

        @Test
        @DisplayName("존재하지 않는 Hub로 상품을 삭제하면 HUB_NOT_FOUND 예외가 발생한다.")
        void deleteProduct_fail_whenHubNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            ProductDeleteCommand command =
                    new ProductDeleteCommand(
                            callerId,
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

            given(productPersistenceService.getProductById(productId))
                    .willReturn(Optional.of(product));

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(hubId))
                    .willThrow(
                            new BusinessException(ErrorCode.HUB_NOT_FOUND)
                    );

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            productCommandService.deleteProduct(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.HUB_NOT_FOUND);

            then(productPersistenceService)
                    .should()
                    .getProductById(productId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(productPersistenceService)
                    .should(never())
                    .deleteProduct(
                            any(UUID.class),
                            any(UUID.class)
                    );
        }
    }
}
