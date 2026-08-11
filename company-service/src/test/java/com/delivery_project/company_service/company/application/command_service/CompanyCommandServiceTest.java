package com.delivery_project.company_service.company.application.command_service;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.application.persistence_service.CompanyPersistenceService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyCommandServiceTest {

    @Mock
    private CompanyPersistenceService companyPersistenceService;

    @Mock
    private CompanyCommandRepository companyCommandRepository;

    @Mock
    private UserPort userPort;

    @Mock
    private HubPort hubPort;

    @InjectMocks
    private CompanyCommandService companyCommandService;

    @Nested
    @DisplayName("업체 생성 외부 비즈니스 로직 테스트")
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

            CompanyCreateResult expectedResult =
                    CompanyCreateResult.from(companyId);

            when(companyPersistenceService.createCompany(
                    hubId,
                    CompanyType.PRODUCER,
                    "테스트 업체",
                    "서울특별시 강남구"
            ))
                    .thenReturn(expectedResult);

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

            verify(companyPersistenceService)
                    .createCompany(
                            hubId,
                            CompanyType.PRODUCER,
                            "테스트 업체",
                            "서울특별시 강남구"
                    );

            verifyNoInteractions(companyCommandRepository);
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

            CompanyCreateResult expectedResult =
                    CompanyCreateResult.from(companyId);

            when(companyPersistenceService.createCompany(
                    hubId,
                    CompanyType.RECEIVER,
                    "테스트 업체",
                    "서울특별시 강남구"
            ))
                    .thenReturn(expectedResult);

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

            verify(companyPersistenceService)
                    .createCompany(
                            hubId,
                            CompanyType.RECEIVER,
                            "테스트 업체",
                            "서울특별시 강남구"
                    );

            verifyNoInteractions(companyCommandRepository);
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

            verifyNoInteractions(companyPersistenceService);
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
                            new BusinessException(
                                    ErrorCode.HUB_NOT_FOUND
                            )
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

            verifyNoInteractions(companyPersistenceService);
            verifyNoInteractions(companyCommandRepository);
        }
    }

    @Nested
    @DisplayName("업체 수정 외부 비즈니스 로직 테스트")
    class UpdateCompanyCommand {

        @Test
        @DisplayName("Master가 업체 수정에 성공한다.")
        void updateCompany_success_whenMaster() {
            // given
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

            CompanyUpdateResult updateResult =
                    new CompanyUpdateResult(companyId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(updatedHubId))
                    .willReturn(mock(HubInfo.class));

            given(companyPersistenceService.updateCompany(
                    company.getId(),
                    updatedHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            ))
                    .willReturn(updateResult);

            // when
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(updatedHubId);

            then(companyPersistenceService)
                    .should()
                    .updateCompany(
                            company.getId(),
                            updatedHubId,
                            CompanyType.RECEIVER,
                            "수정 업체",
                            "수정 주소"
                    );
        }

        @Test
        @DisplayName("담당 Hub Manager가 업체 수정에 성공한다.")
        void updateCompany_success_whenHubManager() {
            // given
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

            CompanyUpdateResult updateResult =
                    new CompanyUpdateResult(companyId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(hubId);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(companyPersistenceService.updateCompany(
                    company.getId(),
                    hubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            ))
                    .willReturn(updateResult);

            // when
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(companyPersistenceService)
                    .should()
                    .updateCompany(
                            company.getId(),
                            hubId,
                            CompanyType.RECEIVER,
                            "수정 업체",
                            "수정 주소"
                    );
        }

        @Test
        @DisplayName("담당 Company Manager가 업체 수정에 성공한다.")
        void updateCompany_success_whenCompanyManager() {
            // given
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

            CompanyUpdateResult updateResult =
                    new CompanyUpdateResult(companyId);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(companyId);

            given(hubPort.getHub(hubId))
                    .willReturn(mock(HubInfo.class));

            given(companyPersistenceService.updateCompany(
                    company.getId(),
                    hubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            ))
                    .willReturn(updateResult);

            // when
            CompanyUpdateResult result =
                    companyCommandService.updateCompany(command);

            // then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(hubId);

            then(companyPersistenceService)
                    .should()
                    .updateCompany(
                            company.getId(),
                            hubId,
                            CompanyType.RECEIVER,
                            "수정 업체",
                            "수정 주소"
                    );
        }

        @Test
        @DisplayName("존재하지 않는 업체를 수정하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateCompany_fail_whenCompanyNotFound() {
            // given
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

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.empty());

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyCommandService.updateCompany(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .shouldHaveNoInteractions();

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(companyPersistenceService)
                    .should(never())
                    .updateCompany(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("업체 수정 권한이 없는 사용자가 요청하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateCompany_fail_whenForbidden() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    companyHubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    companyHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyCommandService.updateCompany(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(companyPersistenceService)
                    .should(never())
                    .updateCompany(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("존재하지 않는 Hub로 업체를 수정하면 HUB_NOT_FOUND 예외가 발생한다.")
        void updateCompany_fail_whenHubNotFound() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID requestedHubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    companyHubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    requestedHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.MASTER);

            given(hubPort.getHub(requestedHubId))
                    .willThrow(
                            new BusinessException(ErrorCode.HUB_NOT_FOUND)
                    );

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyCommandService.updateCompany(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.HUB_NOT_FOUND);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .should()
                    .getHub(requestedHubId);

            then(companyPersistenceService)
                    .should(never())
                    .updateCompany(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("담당하지 않는 Hub의 업체를 Hub Manager가 수정하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateCompany_fail_whenHubManagerOfDifferentHub() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();
            UUID callerHubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    companyHubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    companyHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.HUB_MANAGER);

            given(callerInfo.hubId())
                    .willReturn(callerHubId);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyCommandService.updateCompany(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(companyPersistenceService)
                    .should(never())
                    .updateCompany(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("담당하지 않는 업체를 Company Manager가 수정하면 AUTH_FORBIDDEN 예외가 발생한다.")
        void updateCompany_fail_whenCompanyManagerOfDifferentCompany() {
            // given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            UUID callerCompanyId = UUID.randomUUID();
            UUID companyHubId = UUID.randomUUID();

            Company company = new Company(
                    companyId,
                    companyHubId,
                    "기존 업체",
                    CompanyType.PRODUCER,
                    "기존 주소"
            );

            CompanyUpdateCommand command = new CompanyUpdateCommand(
                    callerId,
                    companyId,
                    companyHubId,
                    CompanyType.RECEIVER,
                    "수정 업체",
                    "수정 주소"
            );

            CallerInfo callerInfo = mock(CallerInfo.class);

            given(companyPersistenceService.getCompanyById(companyId))
                    .willReturn(Optional.of(company));

            given(userPort.getCaller(callerId))
                    .willReturn(callerInfo);

            given(callerInfo.role())
                    .willReturn(Role.COMPANY_MANAGER);

            given(callerInfo.companyId())
                    .willReturn(callerCompanyId);

            // when & then
            BusinessException exception = catchThrowableOfType(
                    () ->
                            companyCommandService.updateCompany(command),
                    BusinessException.class
            );

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

            then(companyPersistenceService)
                    .should()
                    .getCompanyById(companyId);

            then(userPort)
                    .should()
                    .getCaller(callerId);

            then(hubPort)
                    .shouldHaveNoInteractions();

            then(companyPersistenceService)
                    .should(never())
                    .updateCompany(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                    );
        }
    }

    @Nested
    @DisplayName("업체 삭제 외부 비즈니스 로직 테스트")
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

            CompanyDeleteResult deleteResult =
                    new CompanyDeleteResult(
                            companyId,
                            null
                    );

            when(companyPersistenceService.getCompanyById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.MASTER);

            when(hubPort.getHub(hubId))
                    .thenReturn(mock(HubInfo.class));

            when(companyPersistenceService.deleteCompany(
                    company.getId(),
                    callerId
            ))
                    .thenReturn(deleteResult);

            // When
            CompanyDeleteResult result =
                    companyCommandService.deleteCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            verify(companyPersistenceService)
                    .getCompanyById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verify(companyPersistenceService)
                    .deleteCompany(
                            company.getId(),
                            callerId
                    );
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

            CompanyDeleteResult deleteResult =
                    new CompanyDeleteResult(
                            companyId,
                            null
                    );

            when(companyPersistenceService.getCompanyById(companyId))
                    .thenReturn(Optional.of(company));

            when(userPort.getCaller(callerId))
                    .thenReturn(callerInfo);

            when(callerInfo.role())
                    .thenReturn(Role.HUB_MANAGER);

            when(callerInfo.hubId())
                    .thenReturn(hubId);

            when(hubPort.getHub(hubId))
                    .thenReturn(mock(HubInfo.class));

            when(companyPersistenceService.deleteCompany(
                    company.getId(),
                    callerId
            ))
                    .thenReturn(deleteResult);

            // When
            CompanyDeleteResult result =
                    companyCommandService.deleteCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            assertThat(result.companyId())
                    .isEqualTo(companyId);

            verify(companyPersistenceService)
                    .getCompanyById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verify(companyPersistenceService)
                    .deleteCompany(
                            company.getId(),
                            callerId
                    );
        }

        @Test
        @DisplayName("존재하지 않는 업체를 삭제하면 권한 오류가 발생한다.")
        void deleteCompany_fail_whenCompanyNotFound() {
            // Given
            UUID callerId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();

            CompanyDeleteCommand command =
                    new CompanyDeleteCommand(
                            callerId,
                            companyId
                    );

            when(companyPersistenceService.getCompanyById(companyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    companyCommandService.deleteCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.AUTH_FORBIDDEN
                    );

            verify(companyPersistenceService)
                    .getCompanyById(companyId);

            verifyNoInteractions(userPort);
            verifyNoInteractions(hubPort);
        }

        @Test
        @DisplayName("담당하지 않는 Hub의 업체를 Hub Manager가 삭제하면 예외가 발생한다.")
        void deleteCompany_fail_whenHubManagerOfDifferentHub() {
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

            when(companyPersistenceService.getCompanyById(companyId))
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

            verify(companyPersistenceService)
                    .getCompanyById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verifyNoInteractions(hubPort);

            verify(companyPersistenceService, never())
                    .deleteCompany(
                            any(),
                            any()
                    );
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

            when(companyPersistenceService.getCompanyById(companyId))
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

            verify(companyPersistenceService)
                    .getCompanyById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verifyNoInteractions(hubPort);

            verify(companyPersistenceService, never())
                    .deleteCompany(
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("Hub가 존재하지 않으면 업체 삭제에 실패한다.")
        void deleteCompany_fail_whenHubNotFound() {
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

            when(companyPersistenceService.getCompanyById(companyId))
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
                    companyCommandService.deleteCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.HUB_NOT_FOUND
                    );

            verify(companyPersistenceService)
                    .getCompanyById(companyId);

            verify(userPort)
                    .getCaller(callerId);

            verify(hubPort)
                    .getHub(hubId);

            verify(companyPersistenceService, never())
                    .deleteCompany(
                            any(),
                            any()
                    );
        }
    }
}
