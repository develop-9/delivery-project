package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.command.CompanyGetCommand;
import com.delivery_project.company_service.company.application.result.CompanyGetResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyQueryServiceTest {

    @Mock
    private CompanyQueryRepository companyQueryRepository;

    @InjectMocks
    private CompanyQueryService companyQueryService;

    @Nested
    @DisplayName("업체 단건 조회 비즈니스 로직 검증")
    class GetCompanyQuery {

        @Test
        @DisplayName("업체 조회에 성공한다.")
        void getCompany_success() {
            // Given
            UUID companyId = UUID.randomUUID();

            CompanyGetCommand command =
                    new CompanyGetCommand(companyId);

            Company company = Company.builder()
                    .hubId(UUID.randomUUID())
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            when(companyQueryRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            // When
            CompanyGetResult result =
                    companyQueryService.getCompany(command);

            // Then
            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.name())
                    .isEqualTo(company.getName());

            assertThat(result.type())
                    .isEqualTo(company.getType());

            assertThat(result.hubId())
                    .isEqualTo(company.getHubId());

            assertThat(result.address())
                    .isEqualTo(company.getAddress());

            verify(companyQueryRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyQueryRepository);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 조회하면 예외가 발생한다.")
        void getCompany_fail_whenCompanyNotFound() {
            // Given
            UUID companyId = UUID.randomUUID();

            CompanyGetCommand command =
                    new CompanyGetCommand(companyId);

            when(companyQueryRepository.findById(companyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    companyQueryService.getCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.COMPANY_NOT_FOUND
                    );

            verify(companyQueryRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyQueryRepository);
        }
    }
}
