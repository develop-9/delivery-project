package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.command_service.ProductCommandService;
import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.company.application.query.ProductSearchQuery;
import com.delivery_project.company_service.company.application.query_service.ProductQueryService;
import com.delivery_project.company_service.company.application.result.*;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.presentation.request.ProductCreateRequest;
import com.delivery_project.company_service.company.presentation.request.ProductUpdateRequest;
import com.delivery_project.company_service.global.config.SecurityConfig;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ProductApiController.class)
class ProductApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductCommandService productCommandService;

    @MockitoBean
    private ProductQueryService productQueryService;

    @Nested
    @DisplayName("상품 생성 API 테스트")
    class CreateProduct {

        @Test
        @DisplayName("상품을 정상적으로 생성한다")
        void createProduct_success() throws Exception {
            // given
            UUID companyId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductCreateRequest request = new ProductCreateRequest(
                    companyId,
                    "테스트 상품",
                    10000
            );

            ProductCreateResult result = new ProductCreateResult(
                    productId
            );

            given(productCommandService.createProduct(any(ProductCreateCommand.class)))
                    .willReturn(result);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId").value(productId.toString()));

            then(productCommandService)
                    .should()
                    .createProduct(any(ProductCreateCommand.class));
        }

        @Test
        @DisplayName("필수 요청 값이 누락되면 400 Bad Request를 반환한다")
        void createProduct_invalidRequest() throws Exception {
            // given
            ProductCreateRequest request = new ProductCreateRequest(
                    null,
                    null,
                    null
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest());

            then(productCommandService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 업체의 상품을 생성하면 404 Not Found를 반환한다")
        void createProduct_companyNotFound() throws Exception {
            // given
            UUID companyId = UUID.randomUUID();

            ProductCreateRequest request = new ProductCreateRequest(
                    companyId,
                    "테스트 상품",
                    10000
            );

            given(productCommandService.createProduct(any(ProductCreateCommand.class)))
                    .willThrow(
                            new BusinessException(ErrorCode.COMPANY_NOT_FOUND)
                    );

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isNotFound());

            then(productCommandService)
                    .should()
                    .createProduct(any(ProductCreateCommand.class));
        }
    }

    @Nested
    @DisplayName("상품 수정 API 테스트")
    class UpdateProduct {

        @Test
        @DisplayName("상품 정보를 정상적으로 수정한다")
        void updateProduct_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateRequest request = new ProductUpdateRequest(
                    companyId,
                    "수정된 상품",
                    20000
            );

            ProductUpdateResult result = new ProductUpdateResult(
                    productId
            );

            given(productCommandService.updateProduct(
                    any(ProductUpdateCommand.class)
            )).willReturn(result);

            // when
            ResultActions resultActions = mockMvc.perform(
                    put("/api/v1/products/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId")
                            .value(productId.toString()));

            then(productCommandService)
                    .should()
                    .updateProduct(any(ProductUpdateCommand.class));
        }

        @Test
        @DisplayName("상품 수정 요청의 필수 값이 올바르지 않으면 400 Bad Request를 반환한다")
        void updateProduct_invalidRequest() throws Exception {
            // given
            UUID productId = UUID.randomUUID();

            ProductUpdateRequest request = new ProductUpdateRequest(
                    null,
                    null,
                    null
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    put("/api/v1/products/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest());

            then(productCommandService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 404 Not Found를 반환한다")
        void updateProduct_productNotFound() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateRequest request = new ProductUpdateRequest(
                    companyId,
                    "수정된 상품",
                    20000
            );

            given(productCommandService.updateProduct(
                    any(ProductUpdateCommand.class)
            )).willThrow(
                    new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    put("/api/v1/products/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isNotFound());

            then(productCommandService)
                    .should()
                    .updateProduct(any(ProductUpdateCommand.class));
        }

        @Test
        @DisplayName("존재하지 않는 업체로 상품을 수정하면 404 Not Found를 반환한다")
        void updateProduct_companyNotFound() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            ProductUpdateRequest request = new ProductUpdateRequest(
                    companyId,
                    "수정된 상품",
                    20000
            );

            given(productCommandService.updateProduct(
                    any(ProductUpdateCommand.class)
            )).willThrow(
                    new BusinessException(ErrorCode.COMPANY_NOT_FOUND)
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    put("/api/v1/products/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isNotFound());

            then(productCommandService)
                    .should()
                    .updateProduct(any(ProductUpdateCommand.class));
        }
    }

    @Nested
    @DisplayName("상품 삭제 API 테스트")
    class DeleteProduct {

        @Test
        @DisplayName("상품을 정상적으로 삭제한다")
        void deleteProduct_success() throws Exception {
            // given
            UUID productId = UUID.randomUUID();
            Instant deletedAt = Instant.now();

            ProductDeleteResult result = new ProductDeleteResult(
                    productId,
                    deletedAt
            );

            given(productCommandService.deleteProduct(
                    any(ProductDeleteCommand.class)
            )).willReturn(result);

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/products/{productId}", productId)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.productId")
                            .value(productId.toString()))
                    .andExpect(jsonPath("$.data.deletedAt")
                            .value(deletedAt.toString()));

            then(productCommandService)
                    .should()
                    .deleteProduct(any(ProductDeleteCommand.class));
        }

        @Test
        @DisplayName("상품 ID가 UUID 형식이 아니면 400 Bad Request를 반환한다")
        void deleteProduct_invalidProductId() throws Exception {
            // given
            String invalidProductId = "invalid-uuid";

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/products/{productId}", invalidProductId)
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest());

            then(productCommandService)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 404 Not Found를 반환한다")
        void deleteProduct_productNotFound() throws Exception {
            // given
            UUID productId = UUID.randomUUID();

            given(productCommandService.deleteProduct(
                    any(ProductDeleteCommand.class)
            )).willThrow(
                    new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/products/{productId}", productId)
            );

            // then
            resultActions
                    .andExpect(status().isNotFound());

            then(productCommandService)
                    .should()
                    .deleteProduct(any(ProductDeleteCommand.class));
        }
    }

    @Nested
    @DisplayName("상품 단건 조회 API 테스트")
    class GetProduct {
        @Test
        @DisplayName("상품 ID로 상품을 조회할 수 있다.")
        @WithMockUser
        void getProduct_success() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();

            ProductGetResult productGetResult = mock(ProductGetResult.class);

            given(productQueryService.getProduct(any(ProductGetQuery.class)))
                    .willReturn(productGetResult);

            // When & Then
            mockMvc.perform(
                            get("/api/v1/products/{productId}", productId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(productQueryService)
                    .should(times(1))
                    .getProduct(any(ProductGetQuery.class));
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 404를 반환한다.")
        @WithMockUser
        void getProduct_fail_productNotFound() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();

            given(productQueryService.getProduct(any(ProductGetQuery.class)))
                    .willThrow(
                            new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                    );

            // When & Then
            mockMvc.perform(
                            get("/api/v1/products/{productId}", productId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNotFound());

            then(productQueryService)
                    .should(times(1))
                    .getProduct(any(ProductGetQuery.class));
        }

        @Test
        @DisplayName("상품 ID가 UUID 형식이 아니면 400을 반환한다.")
        @WithMockUser
        void getProduct_fail_invalidProductId() throws Exception {
            // Given
            String invalidProductId = "invalid-uuid";

            // When & Then
            mockMvc.perform(
                            get("/api/v1/products/{productId}", invalidProductId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest());

            then(productQueryService)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("상품 목록 조회 / 검색 API 테스트")
    class SearchProduct {
        @Test
        @DisplayName("상품 검색에 성공한다.")
        void searchProduct_success() throws Exception {
            // Given
            int page = 0;
            int size = 10;

            UUID companyId = UUID.randomUUID();

            Product product = Product.builder()
                    .companyId(companyId)
                    .name("테스트 상품")
                    .price(10000)
                    .build();

            ProductSearchResult productSearchResult =
                    new ProductSearchResult(
                            List.of(ProductSearchDataResult.from(product)),
                            page,
                            size,
                            1,
                            1
                    );

            when(productQueryService.searchProduct(any(ProductSearchQuery.class)))
                    .thenReturn(productSearchResult);

            // When
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/products")
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .param("sort", "createdAt,desc")
                            .param("companyId", companyId.toString())
                            .param("name", "테스트")
                            .param("minPrice", "5000")
                            .param("maxPrice", "20000")
            );

            // Then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(productQueryService)
                    .searchProduct(any(ProductSearchQuery.class));
        }

        @Test
        @DisplayName("검색 조건 없이 상품 목록을 조회한다.")
        void searchProduct_success_withoutCondition() throws Exception {
            // Given
            Product product = Product.builder()
                    .companyId(UUID.randomUUID())
                    .name("테스트 상품")
                    .price(10000)
                    .build();

            ProductSearchResult productSearchResult =
                    new ProductSearchResult(
                            List.of(ProductSearchDataResult.from(product)),
                            0,
                            10,
                            1,
                            1
                    );

            when(productQueryService.searchProduct(any(ProductSearchQuery.class)))
                    .thenReturn(productSearchResult);

            // When
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/products")
            );

            // Then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(productQueryService)
                    .searchProduct(any(ProductSearchQuery.class));
        }

        @Test
        @DisplayName("요청한 검색 조건이 Service에 올바르게 전달된다.")
        void searchProduct_requestMapping() throws Exception {
            // Given
            int page = 1;
            int size = 30;

            UUID companyId = UUID.randomUUID();

            ProductSearchResult productSearchResult =
                    new ProductSearchResult(
                            List.of(),
                            page,
                            size,
                            0,
                            0
                    );

            when(productQueryService.searchProduct(any(ProductSearchQuery.class)))
                    .thenReturn(productSearchResult);

            // When
            mockMvc.perform(
                    get("/api/v1/products")
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .param("sort", "createdAt,asc")
                            .param("companyId", companyId.toString())
                            .param("name", "테스트 상품")
                            .param("minPrice", "10000")
                            .param("maxPrice", "50000")
            );

            // Then
            ArgumentCaptor<ProductSearchQuery> queryCaptor =
                    ArgumentCaptor.forClass(ProductSearchQuery.class);

            verify(productQueryService)
                    .searchProduct(queryCaptor.capture());

            ProductSearchQuery query = queryCaptor.getValue();

            assertThat(query.page())
                    .isEqualTo(page);

            assertThat(query.size())
                    .isEqualTo(size);

            assertThat(query.sort())
                    .isEqualTo("createdAt,asc");

            assertThat(query.companyId())
                    .isEqualTo(companyId);

            assertThat(query.name())
                    .isEqualTo("테스트 상품");

            assertThat(query.minPrice())
                    .isEqualTo(10000);

            assertThat(query.maxPrice())
                    .isEqualTo(50000);
        }

        @Test
        @DisplayName("companyId가 UUID 형식이 아니면 상품 검색에 실패한다.")
        void searchProduct_fail_whenInvalidCompanyId() throws Exception {
            // Given
            String invalidCompanyId = "invalid-uuid";

            // When & Then
            mockMvc.perform(
                            get("/api/v1/products")
                                    .param("companyId", invalidCompanyId)
                    )
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productQueryService);
        }

        @Test
        @DisplayName("Service에서 페이지 번호가 유효하지 않으면 상품 검색에 실패한다.")
        void searchProduct_fail_whenInvalidPage() throws Exception {
            // Given
            when(productQueryService.searchProduct(
                    any(ProductSearchQuery.class)
            )).thenThrow(
                    new BusinessException(ErrorCode.INVALID_PAGE)
            );

            // When & Then
            mockMvc.perform(
                            get("/api/v1/products")
                                    .param("page", "-1")
                    )
                    .andExpect(status().isBadRequest());

            verify(productQueryService)
                    .searchProduct(any(ProductSearchQuery.class));
        }
    }
}
