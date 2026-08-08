package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryRouteStatusUpdateCommand;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManagerSequence;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerSequenceCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteCommandServiceTest {

    @Mock
    private DeliveryRouteCommandRepository deliveryRouteCommandRepository;

    @Mock
    private DeliveryManagerCommandRepository deliveryManagerCommandRepository;

    @Mock
    private DeliveryManagerSequenceCommandRepository deliveryManagerSequenceCommandRepository;

    @Mock
    private DeliveryCommandRepository deliveryCommandRepository;

    @InjectMocks
    private DeliveryRouteCommandService deliveryRouteCommandService;

    @Test
    @DisplayName("첫 번째 Route를 시작하면 허브 배송 담당자가 배정되고 Delivery가 HUB_MOVING으로 변경된다")
    void startFirstRouteSuccess() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        DeliveryManager manager = mock(DeliveryManager.class);
        DeliveryManagerSequence sequence =
                mock(DeliveryManagerSequence.class);
        Delivery delivery = mock(Delivery.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null
                );

        when(deliveryRouteCommandRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getId())
                .thenReturn(routeId);

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(route.getSequence())
                .thenReturn(1);

        when(route.getStatus())
                .thenReturn(
                        DeliveryRouteStatus.WAITING,
                        DeliveryRouteStatus.IN_TRANSIT
                );

        when(route.getUpdatedAt())
                .thenReturn(Instant.now());

        when(deliveryRouteCommandRepository
                .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                        deliveryId,
                        DeliveryRouteStatus.IN_TRANSIT
                ))
                .thenReturn(List.of());

        when(deliveryManagerSequenceCommandRepository
                .findForUpdate(
                        DeliveryManagerType.HUB_DELIVERY,
                        null
                ))
                .thenReturn(Optional.of(sequence));

        when(sequence.getLastAssignedSequence())
                .thenReturn(-1);

        when(deliveryManagerCommandRepository
                .findNextAvailableHubManager(-1))
                .thenReturn(Optional.of(manager));

        when(manager.getId())
                .thenReturn(managerId);

        when(manager.getDeliverySequence())
                .thenReturn(0);

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        // when
        DeliveryRouteStatusUpdateResult result =
                deliveryRouteCommandService.updateStatus(command);

        // then
        verify(manager)
                .assignToDelivery();

        verify(route)
                .start(managerId);

        verify(delivery)
                .startHubMoving();

        verify(sequence)
                .updateLastAssignedSequence(0);

        assertEquals(
                DeliveryRouteStatus.WAITING,
                result.previousStatus()
        );

        assertEquals(
                DeliveryRouteStatus.IN_TRANSIT,
                result.currentStatus()
        );
    }

    @Test
    @DisplayName("이미 IN_TRANSIT Route가 존재하면 새로운 Route를 시작할 수 없다")
    void startRouteAlreadyInTransit() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        DeliveryRoute inTransitRoute = mock(DeliveryRoute.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null
                );

        when(deliveryRouteCommandRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(route.getStatus())
                .thenReturn(DeliveryRouteStatus.WAITING);

        when(deliveryRouteCommandRepository
                .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                        deliveryId,
                        DeliveryRouteStatus.IN_TRANSIT
                ))
                .thenReturn(List.of(inTransitRoute));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteCommandService
                                .updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_ROUTE_ALREADY_IN_TRANSIT,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                deliveryManagerSequenceCommandRepository
        );

        verify(deliveryManagerCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("이전 Route가 ARRIVED 상태가 아니면 다음 Route를 시작할 수 없다")
    void startRoutePreviousRouteNotArrived() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        DeliveryRoute previousRoute = mock(DeliveryRoute.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null
                );

        when(deliveryRouteCommandRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(route.getSequence())
                .thenReturn(2);

        when(route.getStatus())
                .thenReturn(DeliveryRouteStatus.WAITING);

        when(deliveryRouteCommandRepository
                .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                        deliveryId,
                        DeliveryRouteStatus.IN_TRANSIT
                ))
                .thenReturn(List.of());

        when(deliveryRouteCommandRepository
                .findByDeliveryIdAndSequenceAndDeletedAtIsNull(
                        deliveryId,
                        1
                ))
                .thenReturn(Optional.of(previousRoute));

        when(previousRoute.getStatus())
                .thenReturn(DeliveryRouteStatus.WAITING);

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteCommandService
                                .updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.PREVIOUS_ROUTE_NOT_ARRIVED,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                deliveryManagerSequenceCommandRepository
        );
    }

    @Test
    @DisplayName("마지막 Route가 도착하면 Delivery를 HUB_ARRIVED로 변경하고 업체 배송 담당자를 배정한다")
    void arriveLastRouteSuccess() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID hubManagerId = UUID.randomUUID();
        UUID companyManagerId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        DeliveryRoute lastRoute = mock(DeliveryRoute.class);

        DeliveryManager hubManager = mock(DeliveryManager.class);
        DeliveryManager companyManager = mock(DeliveryManager.class);

        DeliveryManagerSequence companySequence =
                mock(DeliveryManagerSequence.class);

        Delivery delivery = mock(Delivery.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.ARRIVED,
                        new BigDecimal("125.80"),
                        98
                );

        when(deliveryRouteCommandRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getId())
                .thenReturn(routeId);

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(route.getDeliveryManagerId())
                .thenReturn(hubManagerId);

        when(route.getStatus())
                .thenReturn(
                        DeliveryRouteStatus.IN_TRANSIT,
                        DeliveryRouteStatus.ARRIVED
                );

        when(route.getUpdatedAt())
                .thenReturn(Instant.now());

        when(deliveryManagerCommandRepository
                .findById(hubManagerId))
                .thenReturn(Optional.of(hubManager));

        when(deliveryRouteCommandRepository
                .findLastByDeliveryId(deliveryId))
                .thenReturn(Optional.of(lastRoute));

        when(lastRoute.getId())
                .thenReturn(routeId);

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getDestinationHubId())
                .thenReturn(destinationHubId);

        when(deliveryManagerSequenceCommandRepository
                .findForUpdate(
                        DeliveryManagerType.COMPANY_DELIVERY,
                        destinationHubId
                ))
                .thenReturn(Optional.of(companySequence));

        when(companySequence.getLastAssignedSequence())
                .thenReturn(-1);

        when(deliveryManagerCommandRepository
                .findNextAvailableCompanyManager(
                        destinationHubId,
                        -1
                ))
                .thenReturn(Optional.of(companyManager));

        when(companyManager.getId())
                .thenReturn(companyManagerId);

        when(companyManager.getDeliverySequence())
                .thenReturn(0);

        // when
        DeliveryRouteStatusUpdateResult result =
                deliveryRouteCommandService.updateStatus(command);

        // then
        verify(route)
                .arrive(
                        new BigDecimal("125.80"),
                        98
                );

        verify(hubManager)
                .releaseFromDelivery();

        verify(delivery)
                .arriveAtDestinationHub();

        verify(companyManager)
                .assignToDelivery();

        verify(delivery)
                .assignCompanyDeliveryManager(
                        companyManagerId
                );

        verify(companySequence)
                .updateLastAssignedSequence(0);

        assertEquals(
                DeliveryRouteStatus.IN_TRANSIT,
                result.previousStatus()
        );

        assertEquals(
                DeliveryRouteStatus.ARRIVED,
                result.currentStatus()
        );
    }
}