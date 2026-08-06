package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyGetCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.command_service.CompanyCommandService;
import com.delivery_project.company_service.company.application.query_service.CompanyQueryService;
import com.delivery_project.company_service.company.application.result.*;
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

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(CompanyApiController.class)
class CompanyApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyCommandService companyCommandService;

    @MockitoBean
    private CompanyQueryService companyQueryService;

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
                            any(CompanyUpdateCommand.class)
                    );
        }
    }

    @Nested
    @DisplayName("업체 삭제 API 테스트")
    class DeleteCompany {
        @Test
        @DisplayName("업체 삭제에 성공한다.")
        void deleteCompany_success() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();
            Instant deletedAt = Instant.now();

            CompanyDeleteResult result =
                    CompanyDeleteResult.from(
                            companyId,
                            deletedAt
                    );

            when(companyCommandService.deleteCompany(
                    any(CompanyDeleteCommand.class)
            )).thenReturn(result);

            // When & Then
            mockMvc.perform(
                            delete("/api/v1/companies/{companyId}", companyId)
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.companyId")
                                    .value(companyId.toString())
                    )
                    .andExpect(
                            jsonPath("$.data.deletedAt")
                                    .exists()
                    );

            verify(companyCommandService)
                    .deleteCompany(
                            argThat(command ->
                                    command.companyId().equals(companyId)
                            )
                    );
        }


        @Test
        @DisplayName("존재하지 않는 업체를 삭제하면 404 Not Found를 반환한다.")
        void deleteCompany_fail_whenCompanyNotFound() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();

            when(companyCommandService.deleteCompany(
                    any(CompanyDeleteCommand.class)
            )).thenThrow(
                    new BusinessException(ErrorCode.COMPANY_NOT_FOUND)
            );

            // When & Then
            mockMvc.perform(
                            delete("/api/v1/companies/{companyId}", companyId)
                    )
                    .andExpect(status().isNotFound());

            verify(companyCommandService)
                    .deleteCompany(
                            argThat(command ->
                                    command.companyId().equals(companyId)
                            )
                    );
        }

    }

    @Nested
    @DisplayName("업체 단건 조회 API 테스트")
    class GetCompany {
        @Test
        @DisplayName("업체 조회에 성공한다.")
        void getCompany_success() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyGetResult result = new CompanyGetResult(
                    companyId,
                    "테스트 업체",
                    CompanyType.PRODUCER,
                    hubId,
                    "서울특별시 강남구"
            );

            when(companyQueryService.getCompany(
                    any(CompanyGetCommand.class)
            )).thenReturn(result);

            // When & Then
            mockMvc.perform(
                            get("/api/v1/companies/{companyId}", companyId)
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.data.companyId")
                                    .value(companyId.toString())
                    )
                    .andExpect(
                            jsonPath("$.data.hubId")
                                    .value(hubId.toString())
                    )
                    .andExpect(
                            jsonPath("$.data.type")
                                    .value("PRODUCER")
                    )
                    .andExpect(
                            jsonPath("$.data.name")
                                    .value("테스트 업체")
                    )
                    .andExpect(
                            jsonPath("$.data.address")
                                    .value("서울특별시 강남구")
                    );

            verify(companyQueryService)
                    .getCompany(
                            argThat(command ->
                                    command.companyId().equals(companyId)
                            )
                    );
        }


        @Test
        @DisplayName("존재하지 않는 업체를 조회하면 404 Not Found를 반환한다.")
        void getCompany_fail_whenCompanyNotFound() throws Exception {

            // Given
            UUID companyId = UUID.randomUUID();

            when(companyQueryService.getCompany(
                    any(CompanyGetCommand.class)
            )).thenThrow(
                    new BusinessException(ErrorCode.COMPANY_NOT_FOUND)
            );

            // When & Then
            mockMvc.perform(
                            get("/api/v1/companies/{companyId}", companyId)
                    )
                    .andExpect(status().isNotFound());

            verify(companyQueryService)
                    .getCompany(
                            argThat(command ->
                                    command.companyId().equals(companyId)
                            )
                    );
        }
    }

    @Nested
    @DisplayName("업체 목록 조회/검색 API 테스트")
    class GetAllCompany {
        @Test
        @DisplayName("업체 목록을 검색한다.")
        void getAllCompany_success() throws Exception {

            // Given
            UUID hubId = UUID.randomUUID();

            CompanyGetAllResult result = mock(CompanyGetAllResult.class);

            when(result.content())
                    .thenReturn(Collections.emptyList());

            when(result.page())
                    .thenReturn(0);

            when(result.size())
                    .thenReturn(10);

            when(result.totalElements())
                    .thenReturn(0L);

            when(result.totalPages())
                    .thenReturn(0);

            when(companyQueryService.getAllCompany(any()))
                    .thenReturn(result);

            // When
            mockMvc.perform(
                            get("/api/v1/companies")
                                    .param("page", "0")
                                    .param("size", "10")
                                    .param("sort", "createdAt,desc")
                                    .param("companyName", "테스트")
                                    .param("companyType", "PRODUCER")
                                    .param("hubId", hubId.toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )

                    // Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.totalPages").value(0))
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(companyQueryService)
                    .getAllCompany(any());

            verifyNoMoreInteractions(companyQueryService);
        }


        @Test
        @DisplayName("검색 조건 없이 기본값으로 업체 목록을 검색한다.")
        void getAllCompany_success_withDefaultParameter() throws Exception {

            // Given
            CompanyGetAllResult result = mock(CompanyGetAllResult.class);

            when(result.content())
                    .thenReturn(Collections.emptyList());

            when(result.page())
                    .thenReturn(0);

            when(result.size())
                    .thenReturn(10);

            when(result.totalElements())
                    .thenReturn(0L);

            when(result.totalPages())
                    .thenReturn(0);

            when(companyQueryService.getAllCompany(any()))
                    .thenReturn(result);

            // When
            mockMvc.perform(
                            get("/api/v1/companies")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )

                    // Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.totalPages").value(0))
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(companyQueryService)
                    .getAllCompany(any());

            verifyNoMoreInteractions(companyQueryService);
        }


        @Test
        @DisplayName("잘못된 업체 유형으로 요청하면 400을 반환한다.")
        void getAllCompany_fail_whenInvalidCompanyType() throws Exception {

            // Given
            String invalidCompanyType = "INVALID_TYPE";

            // When
            mockMvc.perform(
                            get("/api/v1/companies")
                                    .param("page", "0")
                                    .param("size", "10")
                                    .param("sort", "createdAt,desc")
                                    .param("companyType", invalidCompanyType)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )

                    // Then
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(companyQueryService);
        }

        @Test
        @DisplayName("잘못된 page 타입으로 요청하면 400을 반환한다.")
        void getAllCompany_fail_whenInvalidPage() throws Exception {

            // Given
            String invalidPage = "abc";

            // When
            mockMvc.perform(
                            get("/api/v1/companies")
                                    .param("page", invalidPage)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )

                    // Then
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(companyQueryService);
        }
    }
}
