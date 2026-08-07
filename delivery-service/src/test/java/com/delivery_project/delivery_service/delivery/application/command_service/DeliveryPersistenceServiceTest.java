package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryCreateCommand;
import com.delivery_project.delivery_service.delivery.application.port.DeliveryCreationLockPort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryCreateResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPathSegment;
import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}