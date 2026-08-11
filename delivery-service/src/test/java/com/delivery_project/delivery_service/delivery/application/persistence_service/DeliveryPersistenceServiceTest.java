package com.delivery_project.delivery_service.delivery.application.persistence_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryCreateCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryUpdateCommand;
import com.delivery_project.delivery_service.delivery.application.port.DeliveryCreationLockPort;
import com.delivery_project.delivery_service.delivery.application.result.*;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import com.delivery_project.delivery_service.global.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryPersistenceServiceTest {

    @Mock
    private DeliveryCommandRepository deliveryCommandRepository;

    @Mock
    private DeliveryRouteCommandRepository deliveryRouteCommandRepository;

    @Mock
    private DeliveryCreationLockPort deliveryCreationLockPort;

    @InjectMocks
    private DeliveryPersistenceService deliveryPersistenceService;

    @Test
    @DisplayName("Delivery 저장 후 생성된 ID를 이용해 DeliveryRoute들을 생성하고 저장한다")
    void createDeliveryWithRoutesSuccess() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID middleHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        DeliveryCreateCommand command =
                new DeliveryCreateCommand(
                        orderId,
                        departureHubId,
                        destinationHubId,
                        "서울특별시 중구 테스트로 1",
                        receiverUserId
                );

        ReceiverInfo receiver =
                new ReceiverInfo(
                        receiverUserId,
                        "홍길동",
                        "slack-test"
                );

        DeliveryPath deliveryPath =
                new DeliveryPath(
                        List.of(
                                new DeliveryPathSegment(
                                        1,
                                        departureHubId,
                                        middleHubId,
                                        new BigDecimal("100.50"),
                                        60
                                ),
                                new DeliveryPathSegment(
                                        2,
                                        middleHubId,
                                        destinationHubId,
                                        new BigDecimal("80.25"),
                                        45
                                )
                        )
                );
        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(java.util.Optional.empty());

        when(deliveryCommandRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> {
                    Delivery delivery =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            delivery,
                            "id",
                            deliveryId
                    );

                    return delivery;
                });

        // when
        DeliveryCreateResult result =
                deliveryPersistenceService.create(
                        command,
                        receiver,
                        deliveryPath
                );

        // then
        ArgumentCaptor<List<DeliveryRoute>> routesCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(deliveryCreationLockPort)
                .lock(orderId);

        verify(deliveryCommandRepository)
                .save(any(Delivery.class));

        verify(deliveryRouteCommandRepository)
                .saveAll(routesCaptor.capture());

        List<DeliveryRoute> savedRoutes =
                routesCaptor.getValue();

        assertEquals(2, savedRoutes.size());

        assertEquals(
                deliveryId,
                savedRoutes.get(0).getDeliveryId()
        );

        assertEquals(
                deliveryId,
                savedRoutes.get(1).getDeliveryId()
        );

        assertEquals(
                1,
                savedRoutes.get(0).getSequence()
        );

        assertEquals(
                2,
                savedRoutes.get(1).getSequence()
        );

        assertEquals(
                departureHubId,
                savedRoutes.get(0).getDepartureHubId()
        );

        assertEquals(
                middleHubId,
                savedRoutes.get(0).getArrivalHubId()
        );

        assertEquals(
                middleHubId,
                savedRoutes.get(1).getDepartureHubId()
        );

        assertEquals(
                destinationHubId,
                savedRoutes.get(1).getArrivalHubId()
        );

        assertEquals(
                deliveryId,
                result.deliveryId()
        );

        assertEquals(
                orderId,
                result.orderId()
        );

        assertEquals(
                DeliveryStatus.PENDING,
                result.status()
        );

        assertEquals(
                2,
                result.routeCount()
        );
    }

    @Test
    @DisplayName("배송 수정 시 Delivery를 다시 조회하여 정보를 변경하고 저장한다")
    void updateDeliverySuccess() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();

        Delivery delivery =
                Delivery.create(
                        orderId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기존 배송지",
                        "기존 수령인",
                        "old-slack"
                );

        ReflectionTestUtils.setField(
                delivery,
                "id",
                deliveryId
        );

        DeliveryUpdateCommand command =
                new DeliveryUpdateCommand(
                        deliveryId,
                        "변경된 배송지",
                        receiverUserId,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        ReceiverInfo receiver =
                new ReceiverInfo(
                        receiverUserId,
                        "변경된 수령인",
                        "new-slack"
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(java.util.Optional.of(delivery));

        when(deliveryCommandRepository.save(delivery))
                .thenReturn(delivery);

        // when
        DeliveryUpdateResult result =
                deliveryPersistenceService.update(
                        command,
                        receiver
                );

        // then
        assertEquals(
                "변경된 배송지",
                delivery.getDeliveryAddress()
        );

        assertEquals(
                "변경된 수령인",
                delivery.getReceiverName()
        );

        assertEquals(
                "new-slack",
                delivery.getReceiverSlackId()
        );

        assertEquals(
                deliveryId,
                result.deliveryId()
        );

        verify(deliveryCommandRepository)
                .findById(deliveryId);

        verify(deliveryCommandRepository)
                .save(delivery);
    }

    @Test
    @DisplayName("수정할 배송이 존재하지 않으면 배송 수정에 실패한다")
    void updateDeliveryNotFound() {
        // given
        UUID deliveryId = UUID.randomUUID();

        DeliveryUpdateCommand command =
                new DeliveryUpdateCommand(
                        deliveryId,
                        "변경된 배송지",
                        null,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(java.util.Optional.empty());

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryPersistenceService.update(
                                command,
                                null
                        )
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(deliveryCommandRepository, never())
                .save(any());
    }
}