package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.port.HubPort;
import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.application.port.dto.HubInfo;
import com.delivery_project.company_service.company.application.result.CompanyCreateResult;
import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;
import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.repository.CompanyCommandRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import com.delivery_project.company_service.global.security.Role;
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

    @Mock
    private UserPort userPort;

    @Mock
    private HubPort hubPort;

    @InjectMocks
    private CompanyCommandService companyCommandService;

    @Nested
    @DisplayName("업체 생성 비즈니스 로직 테스트")
    class CreateCompanyCommand {

        @Test
        @DisplayName("Master가 업체 생성에 성공한다.")
        void createCompany_success_whenMaster() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyCreateCommand command = new CompanyCreateCommand(
                    callerId,
                    hubId,
                    CompanyType.PRODUCER,
                    "테스트 업체",
                    "서울특별시 강남구"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(callerInfo.role())
                    .thenReturn(Role.MASTER);

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(hubPort.getHub(hubId))
                    .thenReturn(mock(HubInfo.class));

            Company savedCompany = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    savedCompany,
                    "id",
                    companyId
            );

            when(companyCommandRepository.save(any(Company.class)))
                    .thenReturn(savedCompany);

            // When
            CompanyCreateResult result =
                    companyCommandService.createCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verify(companyCommandRepository)
                    .save(any(Company.class));
        }

        @Test
        @DisplayName("담당 Hub Manager가 업체 생성에 성공한다.")
        void createCompany_success_whenHubManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyCreateCommand command = new CompanyCreateCommand(
                    callerId,
                    hubId,
                    CompanyType.RECEIVER,
                    "테스트 업체",
                    "서울특별시 강남구"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(hubId);

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(hubPort.getHub(hubId))
                    .thenReturn(mock(HubInfo.class));

            Company savedCompany = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.RECEIVER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    savedCompany,
                    "id",
                    companyId
            );

            when(companyCommandRepository.save(any(Company.class)))
                    .thenReturn(savedCompany);

            // When
            CompanyCreateResult result =
                    companyCommandService.createCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verify(companyCommandRepository)
                    .save(any(Company.class));
        }

        @Test
        @DisplayName("업체 생성 권한이 없으면 업체 생성에 실패한다.")
        void createCompany_fail_whenForbidden() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyCreateCommand command = new CompanyCreateCommand(
                    callerId,
                    hubId,
                    CompanyType.PRODUCER,
                    "테스트 업체",
                    "서울특별시 강남구"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(UUID.randomUUID());

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.createCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.AUTH_FORBIDDEN
                    );

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort, never())
                    .getHub(any());

            verifyNoInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("업체 생성 요청의 Hub가 존재하지 않으면 업체 생성에 실패한다.")
        void createCompany_fail_whenHubNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyCreateCommand command = new CompanyCreateCommand(
                    callerId,
                    hubId,
                    CompanyType.PRODUCER,
                    "테스트 업체",
                    "서울특별시 강남구"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(callerInfo.role())
                    .thenReturn(Role.MASTER);

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(hubPort.getHub(hubId))
                    .thenThrow(
                            new BusinessException(ErrorCode.HUB_NOT_FOUND)
                    );

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.createCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.HUB_NOT_FOUND
                    );

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verifyNoInteractions(companyCommandRepository);
        }
    }

    @Nested
    @DisplayName("업체 수정 비즈니스 로직 테스트")
    class UpdateCompanyCommand {

        @Test
        @DisplayName("Master가 업체 수정에 성공한다.")
        void updateCompany_success_whenMaster() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();
            UUID updatedHubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    hubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    updatedHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.MASTER);

            when(hubPort.getHub(updatedHubId))
                    .thenReturn(mock(HubInfo.class));

            // When
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

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

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(updatedHubId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("담당 Hub Manager가 업체 수정에 성공한다.")
        void updateCompany_success_whenHubManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    hubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    hubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(hubId);

            when(hubPort.getHub(hubId))
                    .thenReturn(mock(HubInfo.class));

            // When
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(company.getHubId())
                    .isEqualTo(hubId);

            assertThat(company.getType())
                    .isEqualTo(CompanyType.RECEIVER);

            assertThat(company.getName())
                    .isEqualTo("수정 업체");

            assertThat(company.getAddress())
                    .isEqualTo("수정 주소");

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("담당 Company Manager가 업체 수정에 성공한다.")
        void updateCompany_success_whenCompanyManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    hubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    hubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.COMPANY_MANAGER);

            when(callerInfo.companyId())
                    .thenReturn(companyId);

            when(hubPort.getHub(hubId))
                    .thenReturn(mock(HubInfo.class));

            // When
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(company.getHubId())
                    .isEqualTo(hubId);

            assertThat(company.getType())
                    .isEqualTo(CompanyType.RECEIVER);

            assertThat(company.getName())
                    .isEqualTo("수정 업체");

            assertThat(company.getAddress())
                    .isEqualTo("수정 주소");

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 수정하면 예외가 발생한다.")
        void updateCompany_fail_whenCompanyNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    hubId,
                    CompanyType.PRODUCER,
                    "수정 업체",
                    "수정 주소"
            );

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.updateCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.COMPANY_NOT_FOUND
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verifyNoInteractions(userPort);
            verifyNoInteractions(hubPort);
        }

        @Test
        @DisplayName("업체 수정 권한이 없으면 예외가 발생한다.")
        void updateCompany_fail_whenForbidden() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("기존 업체")
                    .address("기존 주소")
                    .build();

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    hubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(UUID.randomUUID());

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.updateCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.AUTH_FORBIDDEN
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort, never())
                    .getHub(any());
        }

        @Test
        @DisplayName("존재하지 않는 Hub로 업체를 수정하면 예외가 발생한다.")
        void updateCompany_fail_whenHubNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("기존 업체")
                    .address("기존 주소")
                    .build();

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    hubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.MASTER);

            when(hubPort.getHub(hubId))
                    .thenThrow(
                            new BusinessException(ErrorCode.HUB_NOT_FOUND)
                    );

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.updateCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.HUB_NOT_FOUND
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);
        }
    }

    @Nested
    @DisplayName("업체 삭제 비즈니스 로직 테스트")
    class DeleteCompanyCommand {

        @Test
        @DisplayName("Master가 업체 삭제에 성공한다.")
        void deleteCompany_success_whenMaster() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(
                            callerId,
                            companyId
                    );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.MASTER);

            // When
            CompanyDeleteResult result =
                    companyCommandService.deleteCompany(command);

            // Then
            assertThat(company.getDeletedAt())
                    .isNotNull();

            assertThat(company.getDeletedBy())
                    .isEqualTo(callerId);

            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.deletedAt())
                    .isEqualTo(company.getDeletedAt());

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("담당 Hub Manager가 업체 삭제에 성공한다.")
        void deleteCompany_success_whenHubManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(
                            callerId,
                            companyId
                    );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(hubId);

            // When
            CompanyDeleteResult result =
                    companyCommandService.deleteCompany(command);

            // Then
            assertThat(company.getDeletedAt())
                    .isNotNull();

            assertThat(company.getDeletedBy())
                    .isEqualTo(callerId);

            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.deletedAt())
                    .isEqualTo(company.getDeletedAt());

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verifyNoMoreInteractions(companyCommandRepository);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 삭제하면 예외가 발생한다.")
        void deleteCompany_fail_whenCompanyNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(
                            callerId,
                            companyId
                    );

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

            verifyNoInteractions(userPort);
        }

        @Test
        @DisplayName("업체 삭제 권한이 없으면 예외가 발생한다.")
        void deleteCompany_fail_whenForbidden() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(
                            callerId,
                            companyId
                    );

            Company company = Company.builder()
                    .hubId(companyHubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(callerHubId);

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.deleteCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.AUTH_FORBIDDEN
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);
        }

        @Test
        @DisplayName("Company Manager는 업체를 삭제할 수 없다.")
        void deleteCompany_fail_whenCompanyManager() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(
                            callerId,
                            companyId
                    );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            when(companyCommandRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.COMPANY_MANAGER);

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.deleteCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.AUTH_FORBIDDEN
                    );

            verify(companyCommandRepository)
                    .findById(companyId);

            verify(userPort)
                    .getCaller(callerId);
        }
    }
}
