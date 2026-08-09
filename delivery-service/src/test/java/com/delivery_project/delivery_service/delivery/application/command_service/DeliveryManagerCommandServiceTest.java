package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerInternalDeleteCommand;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerSequenceCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import com.delivery_project.delivery_service.global.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerCommandServiceTest {

    @Mock
    private DeliveryManagerCommandRepository deliveryManagerCommandRepository;

    @Mock
    private DeliveryManagerSequenceCommandRepository deliveryManagerSequenceCommandRepository;

    @Mock
    private DeliveryCommandRepository deliveryCommandRepository;

    @Mock
    private DeliveryRouteCommandRepository deliveryRouteCommandRepository;

    @InjectMocks
    private DeliveryManagerCommandService deliveryManagerCommandService;

    @Test
    @DisplayName("업체 배송 담당자가 진행 중 배송에 배정되어 있으면 삭제할 수 없다")
    void deleteCompanyManagerWithActiveDeliveryFails() {
        UUID managerId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();

        DeliveryManager manager = mock(DeliveryManager.class);

        when(manager.getId()).thenReturn(managerId);
        when(manager.getType())
                .thenReturn(DeliveryManagerType.COMPANY_DELIVERY);

        when(deliveryManagerCommandRepository.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(deliveryCommandRepository
                .existsActiveByCompanyDeliveryManagerId(managerId))
                .thenReturn(true);

        DeliveryManagerDeleteCommand command =
                new DeliveryManagerDeleteCommand(
                        managerId,
                        deletedBy,
                        Role.MASTER
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryManagerCommandService.delete(command)
                );

        assertEquals(
                ErrorCode.ACTIVE_DELIVERY_EXISTS,
                exception.getErrorCode()
        );

        verify(manager, never())
                .deleteManager(any());
    }

    @Test
    @DisplayName("허브 배송 담당자가 진행 중 배송 경로에 배정되어 있으면 삭제할 수 없다")
    void deleteHubManagerWithActiveRouteFails() {
        UUID managerId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();

        DeliveryManager manager = mock(DeliveryManager.class);

        when(manager.getId()).thenReturn(managerId);
        when(manager.getType())
                .thenReturn(DeliveryManagerType.HUB_DELIVERY);

        when(deliveryManagerCommandRepository.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(deliveryRouteCommandRepository
                .existsInTransitByDeliveryManagerId(managerId))
                .thenReturn(true);

        DeliveryManagerDeleteCommand command =
                new DeliveryManagerDeleteCommand(
                        managerId,
                        deletedBy,
                        Role.MASTER
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryManagerCommandService.delete(command)
                );

        assertEquals(
                ErrorCode.ACTIVE_DELIVERY_EXISTS,
                exception.getErrorCode()
        );

        verify(manager, never())
                .deleteManager(any());
    }

    @Test
    @DisplayName("업체 배송 담당자가 진행 중 배송에 배정되어 있지 않으면 삭제할 수 있다")
    void deleteCompanyManagerWithoutActiveDeliverySucceeds() {
        UUID managerId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();

        DeliveryManager manager = mock(DeliveryManager.class);

        when(manager.getId()).thenReturn(managerId);
        when(manager.getType())
                .thenReturn(DeliveryManagerType.COMPANY_DELIVERY);

        when(deliveryManagerCommandRepository.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(deliveryCommandRepository
                .existsActiveByCompanyDeliveryManagerId(managerId))
                .thenReturn(false);

        DeliveryManagerDeleteCommand command =
                new DeliveryManagerDeleteCommand(
                        managerId,
                        deletedBy,
                        Role.MASTER
                );

        deliveryManagerCommandService.delete(command);

        verify(manager)
                .deleteManager(deletedBy);
    }

    @Test
    @DisplayName("진행 중 배송에 배정된 담당자는 내부 사용자 삭제 요청으로도 삭제할 수 없다")
    void internalDeleteWithActiveAssignmentFails() {
        UUID userId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        DeliveryManager manager = mock(DeliveryManager.class);

        when(manager.getId()).thenReturn(managerId);
        when(manager.getType())
                .thenReturn(DeliveryManagerType.HUB_DELIVERY);

        when(deliveryManagerCommandRepository.findByUserId(userId))
                .thenReturn(Optional.of(manager));

        when(deliveryRouteCommandRepository
                .existsInTransitByDeliveryManagerId(managerId))
                .thenReturn(true);

        DeliveryManagerInternalDeleteCommand command =
                new DeliveryManagerInternalDeleteCommand(
                        userId
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryManagerCommandService
                                .deleteByUserId(command)
                );

        assertEquals(
                ErrorCode.ACTIVE_DELIVERY_EXISTS,
                exception.getErrorCode()
        );

        verify(manager, never())
                .deleteManager(any());
    }
}