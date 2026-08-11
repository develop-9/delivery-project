package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryRouteStatusUpdateCommand;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
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
import com.delivery_project.delivery_service.global.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private UserPort userPort;

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
                        null,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
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
                        null,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
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
                        null,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
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
                        98,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
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

    @Test
    @DisplayName("COMPANY_MANAGER는 배송 경로 상태를 변경할 수 없다")
    void updateRouteStatusCompanyManagerForbidden() {
        // given
        UUID routeId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null,
                        UUID.randomUUID(),
                        Role.COMPANY_MANAGER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
                .thenReturn(Optional.of(route));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 배송 경로를 IN_TRANSIT 상태로 시작할 수 없다")
    void startRouteDeliveryManagerForbidden() {
        // given
        UUID routeId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null,
                        UUID.randomUUID(),
                        Role.DELIVERY_MANAGER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
                .thenReturn(Optional.of(route));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 본인이 담당하지 않은 Route를 완료할 수 없다")
    void arriveRouteOtherDeliveryManagerForbidden() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID assignedManagerId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        DeliveryManager requesterManager = mock(DeliveryManager.class);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.ARRIVED,
                        new BigDecimal("100.0"),
                        60,
                        requesterId,
                        Role.DELIVERY_MANAGER
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
                .thenReturn(Optional.of(route));

        when(deliveryManagerCommandRepository.findByUserId(requesterId))
                .thenReturn(Optional.of(requesterManager));

        when(requesterManager.getId())
                .thenReturn(UUID.randomUUID());

        when(route.getDeliveryManagerId())
                .thenReturn(assignedManagerId);

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("HUB_MANAGER는 출발 허브의 배송 경로를 시작할 수 있다")
    void updateStatusHubManagerDepartureHubSuccess() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        DeliveryRoute route =
                DeliveryRoute.create(
                        deliveryId,
                        2,
                        hubId,
                        arrivalHubId,
                        new BigDecimal("100.00"),
                        60
                );

        ReflectionTestUtils.setField(
                route,
                "id",
                routeId
        );

        DeliveryRoute previousRoute =
                DeliveryRoute.create(
                        deliveryId,
                        1,
                        UUID.randomUUID(),
                        hubId,
                        new BigDecimal("50.00"),
                        30
                );

        // 이전 Route를 ARRIVED로 만들어줌
        ReflectionTestUtils.setField(
                previousRoute,
                "status",
                DeliveryRouteStatus.ARRIVED
        );

        UserAuthorizationInfo requester =
                new UserAuthorizationInfo(
                        requesterId,
                        hubId,
                        null
                );

        DeliveryManagerSequence sequence =
                mock(DeliveryManagerSequence.class);

        DeliveryManager manager =
                mock(DeliveryManager.class);

        when(manager.getId())
                .thenReturn(managerId);

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
                .thenReturn(Optional.of(route));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(requester);

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

        when(deliveryManagerSequenceCommandRepository.findForUpdate(
                DeliveryManagerType.HUB_DELIVERY,
                null
        )).thenReturn(Optional.of(sequence));

        when(deliveryManagerCommandRepository
                .findNextAvailableHubManager(any()))
                .thenReturn(Optional.of(manager));

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null,
                        requesterId,
                        Role.HUB_MANAGER
                );

        // when
        deliveryRouteCommandService.updateStatus(command);

        // then
        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(deliveryRouteCommandRepository)
                .save(route);

        assertEquals(
                DeliveryRouteStatus.IN_TRANSIT,
                route.getStatus()
        );

        assertEquals(
                managerId,
                route.getDeliveryManagerId()
        );
    }

    @Test
    @DisplayName("HUB_MANAGER는 도착 허브의 배송 경로를 시작할 수 있다")
    void updateStatusHubManagerArrivalHubSuccess() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        DeliveryRoute route =
                DeliveryRoute.create(
                        deliveryId,
                        2,
                        departureHubId,
                        arrivalHubId,
                        new BigDecimal("100.00"),
                        60
                );

        ReflectionTestUtils.setField(
                route,
                "id",
                routeId
        );

        DeliveryRoute previousRoute =
                DeliveryRoute.create(
                        deliveryId,
                        1,
                        UUID.randomUUID(),
                        departureHubId,
                        new BigDecimal("50.00"),
                        30
                );

        ReflectionTestUtils.setField(
                previousRoute,
                "status",
                DeliveryRouteStatus.ARRIVED
        );

        UserAuthorizationInfo requester =
                new UserAuthorizationInfo(
                        requesterId,
                        arrivalHubId,
                        null
                );

        DeliveryManagerSequence sequence =
                mock(DeliveryManagerSequence.class);

        DeliveryManager manager =
                mock(DeliveryManager.class);

        when(manager.getId())
                .thenReturn(managerId);

        when(deliveryRouteCommandRepository
                .findByIdForUpdate(routeId))
                .thenReturn(Optional.of(route));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(requester);

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

        when(deliveryManagerSequenceCommandRepository
                .findForUpdate(
                        DeliveryManagerType.HUB_DELIVERY,
                        null
                ))
                .thenReturn(Optional.of(sequence));

        when(deliveryManagerCommandRepository
                .findNextAvailableHubManager(any()))
                .thenReturn(Optional.of(manager));

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null,
                        requesterId,
                        Role.HUB_MANAGER
                );

        // when
        deliveryRouteCommandService.updateStatus(command);

        // then
        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(deliveryRouteCommandRepository)
                .save(route);

        assertEquals(
                DeliveryRouteStatus.IN_TRANSIT,
                route.getStatus()
        );

        assertEquals(
                managerId,
                route.getDeliveryManagerId()
        );
    }


    @Test
    @DisplayName("HUB_MANAGER는 담당 허브와 관련 없는 배송 경로의 상태를 변경할 수 없다")
    void updateStatusHubManagerUnrelatedHubForbidden() {
        // given
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        UUID departureHubId = UUID.randomUUID();
        UUID arrivalHubId = UUID.randomUUID();
        UUID unrelatedHubId = UUID.randomUUID();

        DeliveryRoute route =
                DeliveryRoute.create(
                        deliveryId,
                        1,
                        departureHubId,
                        arrivalHubId,
                        new BigDecimal("100.00"),
                        60
                );

        ReflectionTestUtils.setField(
                route,
                "id",
                routeId
        );

        UserAuthorizationInfo requester =
                new UserAuthorizationInfo(
                        requesterId,
                        unrelatedHubId,
                        null
                );

        when(deliveryRouteCommandRepository.findByIdForUpdate(routeId))
                .thenReturn(Optional.of(route));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(requester);

        DeliveryRouteStatusUpdateCommand command =
                new DeliveryRouteStatusUpdateCommand(
                        routeId,
                        DeliveryRouteStatus.IN_TRANSIT,
                        null,
                        null,
                        requesterId,
                        Role.HUB_MANAGER
                );

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteCommandService
                                .updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(deliveryRouteCommandRepository, never())
                .save(any());
    }
}