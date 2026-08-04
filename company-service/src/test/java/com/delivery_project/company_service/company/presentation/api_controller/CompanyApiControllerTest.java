package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command_service.CompanyCommandService;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.presentation.request.CompanyCreateRequest;
import com.delivery_project.company_service.global.config.SecurityConfig;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(CompanyApiController.class)
class CompanyApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyCommandService companyCommandService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("업체 생성 API 테스트")
    class CreateCompany {
        @Test
        @DisplayName("업체 생성에 성공한다.")
        void createCompany_success() throws Exception {

            // Given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyCreateRequest request = new CompanyCreateRequest(
                    hubId,
                    CompanyType.PRODUCER,
                    "테스트 업체",
                    "서울특별시 강남구"
            );

            CompanyCreateResult result =
                    CompanyCreateResult.from(companyId);

            when(companyCommandService.createCompany(any(CompanyCreateCommand.class)))
                    .thenReturn(result);

            // When & Then
            mockMvc.perform(
                            post("/api/v1/companies")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.companyId")
                            .value(companyId.toString())
                    );

            verify(companyCommandService)
                    .createCompany(any(CompanyCreateCommand.class));
        }

        @Test
        @DisplayName("업체명이 비어있으면 업체 생성에 실패한다.")
        void createCompany_fail_whenNameIsBlank() throws Exception {
            // Given
            CompanyCreateRequest request = new CompanyCreateRequest(
                    UUID.randomUUID(),
                    CompanyType.PRODUCER,
                    "",
                    "서울특별시 강남구"
            );

            // When & Then
            mockMvc.perform(
                            post("/api/v1/companies")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest());

            verify(companyCommandService, never())
                    .createCompany(any(CompanyCreateCommand.class));
        }
    }
}
