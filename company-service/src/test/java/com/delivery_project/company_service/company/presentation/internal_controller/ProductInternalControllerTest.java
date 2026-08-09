package com.delivery_project.company_service.company.presentation.internal_controller;

import com.delivery_project.company_service.company.application.query.InternalProductGetQuery;
import com.delivery_project.company_service.company.application.query_service.ProductQueryService;
import com.delivery_project.company_service.company.application.result.InternalProductGetResult;
import com.delivery_project.company_service.global.config.SecurityConfig;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.delivery_project.company_service.global.security.JwtTokenParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Import(SecurityConfig.class)
@WebMvcTest(ProductInternalController.class)
class ProductInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductQueryService productQueryService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @Nested
    @DisplayName("상품 단건 조회 API 테스트")
    class GetProduct {

        @Test
        @DisplayName("상품 단건 조회에 성공한다.")
        void getProduct_success() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();

            InternalProductGetResult result =
                    new InternalProductGetResult(
                            productId,
                            "테스트 상품",
                            10000
                    );

            when(productQueryService.getProduct(
                    any(InternalProductGetQuery.class)
            )).thenReturn(result);

            // When
            ResultActions resultActions = mockMvc.perform(
                    get("/internal/v1/products/{productId}", productId)
            );

            // Then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(productQueryService)
                    .getProduct(any(InternalProductGetQuery.class));
        }

        @Test
        @DisplayName("상품 ID가 UUID 형식이 아니면 상품 단건 조회에 실패한다.")
        void getProduct_fail_whenInvalidProductId() throws Exception {
            // Given
            String invalidProductId = "invalid-product-id";

            // When & Then
            mockMvc.perform(
                            get("/internal/v1/products/{productId}", invalidProductId)
                    )
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(productQueryService);
        }

        @Test
        @DisplayName("존재하지 않는 상품이면 상품 단건 조회에 실패한다.")
        void getProduct_fail_whenProductNotFound() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();

            when(productQueryService.getProduct(
                    any(InternalProductGetQuery.class)
            )).thenThrow(
                    new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            // When & Then
            mockMvc.perform(
                            get("/internal/v1/products/{productId}", productId)
                    )
                    .andExpect(status().isNotFound());

            verify(productQueryService)
                    .getProduct(any(InternalProductGetQuery.class));
        }
    }
}
