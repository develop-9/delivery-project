package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.InternalProductGetQuery;
import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.company.application.query.ProductSearchQuery;
import com.delivery_project.company_service.company.application.result.InternalProductGetResult;
import com.delivery_project.company_service.company.application.result.ProductGetResult;
import com.delivery_project.company_service.company.application.result.ProductSearchResult;
import com.delivery_project.company_service.company.application.support.pagination.PageValidator;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.infrastructure.persistence.ProductQueryRepositoryImpl;
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
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductQueryRepositoryImpl productQueryRepository;

    @Mock
    private PageValidator pageValidator;

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
            UUID callerId = UUID.randomUUID();

            Product product = mock(Product.class);
            given(product.getId()).willReturn(productId);

            ProductGetQuery productGetQuery =
                    new ProductGetQuery(
                            callerId,
                            Role.MASTER,
                            productId
                    );

            given(productQueryRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // When
            ProductGetResult result =
                    productQueryService.getProduct(productGetQuery);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.productId()).isEqualTo(productId);

            then(productQueryRepository)
                    .should(times(1))
                    .findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다.")
        void getProduct_fail_productNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductGetQuery productGetQuery =
                    new ProductGetQuery(
                            callerId,
                            Role.MASTER,
                            productId
                    );

            given(productQueryRepository.findById(productId))
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

            then(productQueryRepository)
                    .should(times(1))
                    .findById(productId);
        }
    }

    @Nested
    @DisplayName("상품 목록 조회 / 검색 비즈니스 로직 검증")
    class SearchProduct {
        @Test
        @DisplayName("상품 검색에 성공한다.")
        void searchProduct_success() {
            // Given
            int page = 0;
            int size = 10;

            Sort sort = Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );

            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductSearchQuery command = new ProductSearchQuery(
                    callerId,
                    Role.MASTER,
                    page,
                    size,
                    "createdAt,desc",
                    companyId,
                    "테스트",
                    10000,
                    50000
            );

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    sort
            );

            Product product = Product.builder()
                    .companyId(companyId)
                    .name("테스트 상품")
                    .price(20000)
                    .build();

            Page<Product> productPage =
                    new PageImpl<>(
                            List.of(product),
                            pageable,
                            1
                    );

            when(pageValidator.validatePage(page))
                    .thenReturn(page);

            when(pageValidator.normalizeSize(size))
                    .thenReturn(size);

            when(pageValidator.normalizeSort("createdAt,desc"))
                    .thenReturn(sort);

            when(productQueryRepository.search(
                    command.companyId(),
                    command.name(),
                    command.minPrice(),
                    command.maxPrice(),
                    pageable
            )).thenReturn(productPage);

            // When
            ProductSearchResult result =
                    productQueryService.searchProduct(command);

            // Then
            assertThat(result)
                    .isNotNull();

            verify(pageValidator)
                    .validatePage(page);

            verify(pageValidator)
                    .normalizeSize(size);

            verify(pageValidator)
                    .normalizeSort("createdAt,desc");

            verify(productQueryRepository)
                    .search(
                            command.companyId(),
                            command.name(),
                            command.minPrice(),
                            command.maxPrice(),
                            pageable
                    );
        }

        @Test
        @DisplayName("페이지 번호가 유효하지 않으면 상품 검색에 실패한다.")
        void searchProduct_fail_whenInvalidPage() {
            // Given
            Integer page = -1;

            UUID callerId = UUID.randomUUID();

            ProductSearchQuery command = new ProductSearchQuery(
                    callerId,
                    Role.MASTER,
                    page,
                    10,
                    "createdAt,desc",
                    null,
                    null,
                    null,
                    null
            );

            when(pageValidator.validatePage(page))
                    .thenThrow(
                            new BusinessException(ErrorCode.INVALID_PAGE)
                    );

            // When & Then
            assertThatThrownBy(() ->
                    productQueryService.searchProduct(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.INVALID_PAGE
                    );

            verify(pageValidator)
                    .validatePage(page);

            verify(pageValidator, never())
                    .normalizeSize(any());

            verify(pageValidator, never())
                    .normalizeSort(any());

            verifyNoInteractions(productQueryRepository);
        }

        @Test
        @DisplayName("가격 검색 범위가 유효하지 않으면 상품 검색에 실패한다.")
        void searchProduct_fail_whenInvalidPrice() {
            // Given
            int page = 0;
            int size = 10;

            Sort sort = Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );

            UUID callerId = UUID.randomUUID();

            ProductSearchQuery command = new ProductSearchQuery(
                    callerId,
                    Role.MASTER,
                    page,
                    size,
                    "createdAt,desc",
                    null,
                    null,
                    50000,
                    10000
            );

            when(pageValidator.validatePage(page))
                    .thenReturn(page);

            when(pageValidator.normalizeSize(size))
                    .thenReturn(size);

            when(pageValidator.normalizeSort("createdAt,desc"))
                    .thenReturn(sort);

            // When & Then
            assertThatThrownBy(() ->
                    productQueryService.searchProduct(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.PRODUCT_SEARCH_INVALID_PRICE
                    );

            verify(pageValidator)
                    .validatePage(page);

            verifyNoInteractions(productQueryRepository);
        }
    }

    @Nested
    @DisplayName("내부 상품 단건 조회 비즈니스 로직 검증")
    class GetProductForInternal {

        @Test
        @DisplayName("상품 단건 조회에 성공한다.")
        void getProduct_success() {
            // Given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            InternalProductGetQuery query =
                    new InternalProductGetQuery(productId);

            Product product = Product.builder()
                    .companyId(companyId)
                    .name("테스트 상품")
                    .price(10000)
                    .build();

            when(productQueryRepository.findById(productId))
                    .thenReturn(Optional.of(product));

            // When
            InternalProductGetResult result =
                    productQueryService.getProduct(query);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.productId())
                    .isEqualTo(product.getId());

            assertThat(result.name())
                    .isEqualTo("테스트 상품");

            assertThat(result.price())
                    .isEqualTo(10000);

            verify(productQueryRepository)
                    .findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 상품 단건 조회에 실패한다.")
        void getProduct_fail_whenProductNotFound() {
            // Given
            UUID productId = UUID.randomUUID();

            InternalProductGetQuery query =
                    new InternalProductGetQuery(productId);

            when(productQueryRepository.findById(productId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    productQueryService.getProduct(query)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.PRODUCT_NOT_FOUND
                    );

            verify(productQueryRepository)
                    .findById(productId);
        }
    }
}
