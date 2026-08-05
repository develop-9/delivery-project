package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.repository.CompanyCommandRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyCommandServiceTest {

    @Mock
    private CompanyCommandRepository companyCommandRepository;

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

            when(companyCommandRepository.save(any(Company.class)))
                    .thenReturn(savedCompany);

            // When
            CompanyCreateResult result =
                    companyCommandService.createCompany(command);

            // Then
            assertThat(result.companyId())
                    .isEqualTo(companyId);

            verify(companyCommandRepository).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("업체 수정 비즈니스 로직 테스트")
    class UpdateCompanyCommand {

        @Test
        @DisplayName("업체 수정에 성공한다.")
        void updateCompany_success() {
            // Given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID updatedHubId = UUID.randomUUID();

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("기존 업체")
                    .address("기존 주소")
                    .build();

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    companyId,
                    updatedHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            // When
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(
                            command
                    );

            // Then
            assertThat(company.getHubId())
                    .isEqualTo(updatedHubId);
            assertThat(company.getType())
                    .isEqualTo(CompanyType.RECEIVER);
            assertThat(company.getName())
                    .isEqualTo("수정 업체");
            assertThat(company.getAddress())
                    .isEqualTo("수정 주소");

            verify(companyCommandRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 수정하면 예외가 발생한다.")
        void updateCompany_fail_whenCompanyNotFound() {
            // Given
            UUID companyId = UUID.randomUUID();

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    companyId,
                    UUID.randomUUID(),
                    CompanyType.PRODUCER,
                    "수정 업체",
                    "수정 주소"
            );

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.updateCompany(
                            command
                    )
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.COMPANY_NOT_FOUND
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyCommandRepository);
        }
    }

    @Nested
    @DisplayName("업체 삭제 비즈니스 로직 테스트")
    class DeleteCompanyCommand {
        @Test
        @DisplayName("업체 삭제에 성공한다.")
        void deleteCompany_success() {

            // Given
            UUID companyId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(companyId);

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

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            // When
            CompanyDeleteResult result =
                    companyCommandService.deleteCompany(command);

            // Then
            assertThat(company.getDeletedAt())
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.deletedAt())
                    .isEqualTo(company.getDeletedAt());

            verify(companyCommandRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 삭제하면 예외가 발생한다.")
        void deleteCompany_fail_whenCompanyNotFound() {
            // Given
            UUID companyId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(companyId);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.deleteCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.COMPANY_NOT_FOUND
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyCommandRepository);
        }
    }
}
