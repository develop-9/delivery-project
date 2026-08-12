package com.delivery_project.company_service.company.application.persistence_service;

import com.delivery_project.company_service.company.application.port.OrderPort;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.CompanyCommandRepository;
import com.delivery_project.company_service.company.domain.repository.ProductQueryRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CompanyPersistenceServiceTest {

    @Mock
    private CompanyCommandRepository companyCommandRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private OrderPort orderPort;

    @InjectMocks
    private CompanyPersistenceService companyPersistenceService;

    @Nested
    @DisplayName("업체 생성 트랜잭션 비즈니스 로직 테스트")
    class CreateCompanyPersistence {

        @Test
        @DisplayName("업체를 정상적으로 생성한다.")
        void createCompany_success() {
            // given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyType type = CompanyType.PRODUCER;
            String name = "테스트 업체";
            String address = "서울특별시 강남구";

            Company savedCompany = Company.builder()
                    .hubId(hubId)
                    .type(type)
                    .name(name)
                    .address(address)
                    .build();

            ReflectionTestUtils.setField(
                    savedCompany,
                    "id",
                    companyId
            );

            given(companyCommandRepository.save(any(Company.class)))
                    .willReturn(savedCompany);

            // when
            CompanyCreateResult result =
                    companyPersistenceService.createCompany(
                            hubId,
                            type,
                            name,
                            address
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            then(companyCommandRepository)
                    .should()
                    .save(any(Company.class));
        }

        @Test
        @DisplayName("업체 생성 시 입력한 정보가 Company에 정상적으로 전달된다.")
        void createCompany_success_withCompanyInformation() {
            // given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyType type = CompanyType.RECEIVER;
            String name = "테스트 수령 업체";
            String address = "서울특별시 송파구";

            Company savedCompany = Company.builder()
                    .hubId(hubId)
                    .type(type)
                    .name(name)
                    .address(address)
                    .build();

            ReflectionTestUtils.setField(
                    savedCompany,
                    "id",
                    companyId
            );

            given(companyCommandRepository.save(any(Company.class)))
                    .willReturn(savedCompany);

            // when
            CompanyCreateResult result =
                    companyPersistenceService.createCompany(
                            hubId,
                            type,
                            name,
                            address
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            then(companyCommandRepository)
                    .should()
                    .save(argThat(company ->
                            company.getHubId().equals(hubId)
                                    && company.getType() == type
                                    && company.getName().equals(name)
                                    && company.getAddress().equals(address)
                    ));
        }

        @Test
        @DisplayName("저장된 업체의 ID를 생성 결과로 반환한다.")
        void createCompany_returnSavedCompanyId() {
            // given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

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

            given(companyCommandRepository.save(any(Company.class)))
                    .willReturn(company);

            // when
            CompanyCreateResult result =
                    companyPersistenceService.createCompany(
                            hubId,
                            CompanyType.PRODUCER,
                            "테스트 업체",
                            "서울특별시 강남구"
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            then(companyCommandRepository)
                    .should()
                    .save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("업체 수정 트랜잭션 비즈니스 로직 테스트")
    class UpdateCompanyPersistence {

        @Test
        @DisplayName("업체 정보를 정상적으로 수정한다.")
        void updateCompany_success() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID updatedHubId = UUID.randomUUID();

            CompanyType updatedType = CompanyType.RECEIVER;
            String updatedName = "수정 업체";
            String updatedAddress = "서울특별시 송파구";

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("기존 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(companyCommandRepository.save(company))
                    .willReturn(company);

            // when
            CompanyUpdateResult result =
                    companyPersistenceService.updateCompany(
                            companyId,
                            updatedHubId,
                            updatedType,
                            updatedName,
                            updatedAddress
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(company.getHubId())
                    .isEqualTo(updatedHubId);

            assertThat(company.getType())
                    .isEqualTo(updatedType);

            assertThat(company.getName())
                    .isEqualTo(updatedName);

            assertThat(company.getAddress())
                    .isEqualTo(updatedAddress);

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(companyCommandRepository)
                    .should()
                    .save(company);
        }

        @Test
        @DisplayName("업체 수정 시 전달된 모든 정보가 Company에 반영된다.")
        void updateCompany_success_withUpdatedCompanyInformation() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            Company company = Company.builder()
                    .hubId(UUID.randomUUID())
                    .type(CompanyType.PRODUCER)
                    .name("기존 업체")
                    .address("기존 주소")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CompanyType type = CompanyType.RECEIVER;
            String name = "새로운 업체";
            String address = "서울특별시 서초구";

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(companyCommandRepository.save(company))
                    .willReturn(company);

            // when
            CompanyUpdateResult result =
                    companyPersistenceService.updateCompany(
                            companyId,
                            hubId,
                            type,
                            name,
                            address
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(company.getHubId())
                    .isEqualTo(hubId);

            assertThat(company.getType())
                    .isEqualTo(type);

            assertThat(company.getName())
                    .isEqualTo(name);

            assertThat(company.getAddress())
                    .isEqualTo(address);

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(companyCommandRepository)
                    .should()
                    .save(company);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 수정하면 COMPANY_NOT_FOUND 예외가 발생한다.")
        void updateCompany_fail_whenCompanyNotFound() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyType type = CompanyType.RECEIVER;
            String name = "수정 업체";
            String address = "서울특별시 송파구";

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyPersistenceService.updateCompany(
                                    companyId,
                                    hubId,
                                    type,
                                    name,
                                    address
                            ),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(companyCommandRepository)
                    .should(never())
                    .save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("업체 삭제 트랜잭션 비즈니스 로직 테스트")
    class DeleteCompanyPersistence {

        @Test
        @DisplayName("업체와 소속 상품을 정상적으로 논리 삭제한다.")
        void deleteCompany_success() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();

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

            Product product1 = new Product(
                    productId1,
                    companyId,
                    "테스트 상품 1",
                    10000
            );

            Product product2 = new Product(
                    productId2,
                    companyId,
                    "테스트 상품 2",
                    20000
            );

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(productQueryRepository.findByCompanyId(companyId))
                    .willReturn(List.of(product1, product2));

            given(companyCommandRepository.save(company))
                    .willReturn(company);

            // when
            CompanyDeleteResult result =
                    companyPersistenceService.deleteCompany(
                            companyId,
                            callerId
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.deletedAt())
                    .isNotNull();

            assertThat(company.getDeletedAt())
                    .isNotNull();

            assertThat(company.getDeletedBy())
                    .isEqualTo(callerId);

            assertThat(product1.getDeletedAt())
                    .isNotNull();

            assertThat(product1.getDeletedBy())
                    .isEqualTo(callerId);

            assertThat(product2.getDeletedAt())
                    .isNotNull();

            assertThat(product2.getDeletedBy())
                    .isEqualTo(callerId);

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(productQueryRepository)
                    .should()
                    .findByCompanyId(companyId);

            then(orderPort)
                    .should()
                    .deleteInventory(productId1);

            then(orderPort)
                    .should()
                    .deleteInventory(productId2);

            then(companyCommandRepository)
                    .should()
                    .save(company);
        }

        @Test
        @DisplayName("소속 상품이 없는 업체도 정상적으로 논리 삭제한다.")
        void deleteCompany_success_whenProductsNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

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

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(productQueryRepository.findByCompanyId(companyId))
                    .willReturn(List.of());

            given(companyCommandRepository.save(company))
                    .willReturn(company);

            // when
            CompanyDeleteResult result =
                    companyPersistenceService.deleteCompany(
                            companyId,
                            callerId
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.deletedAt())
                    .isNotNull();

            assertThat(company.getDeletedAt())
                    .isNotNull();

            assertThat(company.getDeletedBy())
                    .isEqualTo(callerId);

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(productQueryRepository)
                    .should()
                    .findByCompanyId(companyId);

            then(orderPort)
                    .shouldHaveNoInteractions();

            then(companyCommandRepository)
                    .should()
                    .save(company);
        }

        @Test
        @DisplayName("소속 상품이 여러 개 존재해도 모든 상품의 재고를 삭제하고 논리 삭제한다.")
        void deleteCompany_success_whenMultipleProducts() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            Product product1 = new Product(
                    UUID.randomUUID(),
                    companyId,
                    "테스트 상품 1",
                    10000
            );

            Product product2 = new Product(
                    UUID.randomUUID(),
                    companyId,
                    "테스트 상품 2",
                    20000
            );

            Product product3 = new Product(
                    UUID.randomUUID(),
                    companyId,
                    "테스트 상품 3",
                    30000
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

            List<Product> products = List.of(
                    product1,
                    product2,
                    product3
            );

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(productQueryRepository.findByCompanyId(companyId))
                    .willReturn(products);

            given(companyCommandRepository.save(company))
                    .willReturn(company);

            // when
            CompanyDeleteResult result =
                    companyPersistenceService.deleteCompany(
                            companyId,
                            callerId
                    );

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.deletedAt())
                    .isNotNull();

            assertThat(company.getDeletedAt())
                    .isNotNull();

            assertThat(company.getDeletedBy())
                    .isEqualTo(callerId);

            assertThat(products)
                    .allSatisfy(product -> {
                        assertThat(product.getDeletedAt())
                                .isNotNull();

                        assertThat(product.getDeletedBy())
                                .isEqualTo(callerId);
                    });

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(productQueryRepository)
                    .should()
                    .findByCompanyId(companyId);

            then(orderPort)
                    .should()
                    .deleteInventory(product1.getId());

            then(orderPort)
                    .should()
                    .deleteInventory(product2.getId());

            then(orderPort)
                    .should()
                    .deleteInventory(product3.getId());

            then(companyCommandRepository)
                    .should()
                    .save(company);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 삭제하면 COMPANY_NOT_FOUND 예외가 발생한다.")
        void deleteCompany_fail_whenCompanyNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyPersistenceService.deleteCompany(
                                    companyId,
                                    callerId
                            ),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(productQueryRepository)
                    .shouldHaveNoInteractions();

            then(orderPort)
                    .shouldHaveNoInteractions();

            then(companyCommandRepository)
                    .should(never())
                    .save(any(Company.class));
        }

        @Test
        @DisplayName("상품 재고 삭제에 실패하면 이후 상품 및 업체 삭제가 수행되지 않는다.")
        void deleteCompany_fail_whenInventoryDeleteFailed() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();

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

            Product product1 = new Product(
                    productId1,
                    companyId,
                    "테스트 상품 1",
                    10000
            );

            Product product2 = new Product(
                    productId2,
                    companyId,
                    "테스트 상품 2",
                    20000
            );

            given(companyCommandRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            given(productQueryRepository.findByCompanyId(companyId))
                    .willReturn(List.of(product1, product2));

            willThrow(new RuntimeException("재고 삭제 실패"))
                    .given(orderPort)
                    .deleteInventory(productId1);

            // when & then
            RuntimeException exception = catchThrowableOfType(
                    () ->
                            companyPersistenceService.deleteCompany(
                                    companyId,
                                    callerId
                            ),
                    RuntimeException.class
            );

            assertThat(exception.getMessage())
                    .isEqualTo("재고 삭제 실패");

            assertThat(product1.getDeletedAt())
                    .isNull();

            assertThat(product1.getDeletedBy())
                    .isNull();

            assertThat(product2.getDeletedAt())
                    .isNull();

            assertThat(product2.getDeletedBy())
                    .isNull();

            assertThat(company.getDeletedAt())
                    .isNull();

            assertThat(company.getDeletedBy())
                    .isNull();

            then(companyCommandRepository)
                    .should()
                    .findById(companyId);

            then(productQueryRepository)
                    .should()
                    .findByCompanyId(companyId);

            then(orderPort)
                    .should()
                    .deleteInventory(productId1);

            then(orderPort)
                    .should(never())
                    .deleteInventory(productId2);

            then(companyCommandRepository)
                    .should(never())
                    .save(any(Company.class));
        }
    }
}
