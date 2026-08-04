package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.command_service.CompanyCommandService;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.presentation.request.CompanyCreateRequest;
import com.delivery_project.company_service.company.presentation.request.CompanyUpdateRequest;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Nested
    @DisplayName("업체 수정 API 테스트")
    class UpdateCompany {

        @Test
        @DisplayName("업체 수정에 성공한다.")
        void updateCompany_success() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyUpdateRequest request = new CompanyUpdateRequest(
                    hubId,
                    CompanyType.PRODUCER,
                    "수정 업체",
                    "서울특별시 강남구"
            );

            CompanyUpdateResult result =
                    CompanyUpdateResult.from(companyId);

            when(companyCommandService.updateCompany(
                    eq(companyId),
                    any(CompanyUpdateCommand.class)
            )).thenReturn(result);

            // When & Then
            mockMvc.perform(
                            put("/api/v1/companies/{companyId}", companyId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.companyId")
                                    .value(companyId.toString())
                    );

            verify(companyCommandService)
                    .updateCompany(
                            eq(companyId),
                            any(CompanyUpdateCommand.class)
                    );
        }


        @Test
        @DisplayName("업체 수정 요청의 필수값이 누락되면 400 Bad Request를 반환한다.")
        void updateCompany_fail_whenInvalidRequest() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();

            CompanyUpdateRequest request = new CompanyUpdateRequest(
                    UUID.randomUUID(),
                    CompanyType.PRODUCER,
                    "",
                    "서울특별시 강남구"
            );

            // When & Then
            mockMvc.perform(
                            put("/api/v1/companies/{companyId}", companyId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest());

            verify(companyCommandService, never())
                    .updateCompany(
                            any(UUID.class),
                            any(CompanyUpdateCommand.class)
                    );
        }


        @Test
        @DisplayName("존재하지 않는 업체를 수정하면 업체를 찾을 수 없다는 예외가 발생한다.")
        void updateCompany_fail_whenCompanyNotFound() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyUpdateRequest request = new CompanyUpdateRequest(
                    hubId,
                    CompanyType.PRODUCER,
                    "수정 업체",
                    "서울특별시 강남구"
            );

            when(companyCommandService.updateCompany(
                    eq(companyId),
                    any(CompanyUpdateCommand.class)
            )).thenThrow(
                    new BusinessException(ErrorCode.COMPANY_NOT_FOUND)
            );

            // When & Then
            mockMvc.perform(
                            put("/api/v1/companies/{companyId}", companyId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isNotFound());

            verify(companyCommandService)
                    .updateCompany(
                            eq(companyId),
                            any(CompanyUpdateCommand.class)
                    );
        }
    }
}
