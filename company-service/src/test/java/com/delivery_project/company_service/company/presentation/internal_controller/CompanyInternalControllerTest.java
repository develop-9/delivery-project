package com.delivery_project.company_service.company.presentation.internal_controller;

import com.delivery_project.company_service.company.application.query.InternalCompanyGetQuery;
import com.delivery_project.company_service.company.application.query_service.CompanyQueryService;
import com.delivery_project.company_service.company.application.result.InternalCompanyGetResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Import(SecurityConfig.class)
@WebMvcTest(CompanyInternalController.class)
class CompanyInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyQueryService companyQueryService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @Nested
    @DisplayName("업체 단건 조회 API 테스트")
    class GetCompany {

        @Test
        @DisplayName("업체 ID로 업체를 정상적으로 조회한다")
        void getCompany_success() throws Exception {
            // given
            UUID companyId = UUID.randomUUID();

            InternalCompanyGetResult result =
                    new InternalCompanyGetResult(
                            companyId,
                            "테스트 업체",
                            CompanyType.PRODUCER
                    );

            given(companyQueryService.getCompanyForInternal(
                    new InternalCompanyGetQuery(companyId)
            )).willReturn(result);

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/internal/v1/companies/{companyId}", companyId)
                            .contentType(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.companyId").value(companyId.toString()))
                    .andExpect(jsonPath("$.data.name").value("테스트 업체"))
                    .andExpect(jsonPath("$.data.type").value("PRODUCER"));

            then(companyQueryService)
                    .should()
                    .getCompanyForInternal(new InternalCompanyGetQuery(companyId));
        }

        @Test
        @DisplayName("존재하지 않는 업체 ID로 조회하면 404를 반환한다")
        void getCompany_notFound() throws Exception {
            // given
            UUID companyId = UUID.randomUUID();

            given(companyQueryService.getCompanyForInternal(
                    new InternalCompanyGetQuery(companyId)
            )).willThrow(
                    new BusinessException(ErrorCode.COMPANY_NOT_FOUND)
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/internal/v1/companies/{companyId}", companyId)
                            .contentType(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isNotFound());

            then(companyQueryService)
                    .should()
                    .getCompanyForInternal(new InternalCompanyGetQuery(companyId));
        }
    }
}
