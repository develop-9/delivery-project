package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.command_service.ProductCommandService;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.presentation.request.ProductCreateRequest;
import com.delivery_project.company_service.company.presentation.request.ProductUpdateRequest;
import com.delivery_project.company_service.global.config.SecurityConfig;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
}
