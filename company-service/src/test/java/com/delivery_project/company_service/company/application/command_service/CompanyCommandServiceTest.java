package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyCommandServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyCommandService companyCommandService;

    @Nested
    @DisplayName("업체 생성 비즈니스 로직 테스트")
    class CreateCompanyCommand {
        @Test
        @DisplayName("업체 생성에 성공한다.")
        void createCompany_success() {
            // Given
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyCreateCommand command = new CompanyCreateCommand(
                    hubId,
                    CompanyType.PRODUCER,
                    "테스트 업체",
                    "서울특별시 강남구"
            );

            Company savedCompany = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(savedCompany, "id", companyId);

            when(companyRepository.save(any(Company.class)))
                    .thenReturn(savedCompany);

            // When
            CompanyCreateResult result =
                    companyCommandService.createCompany(command);

            // Then
            assertThat(result.companyId())
                    .isEqualTo(companyId);

            verify(companyRepository).save(any(Company.class));
        }
    }
}