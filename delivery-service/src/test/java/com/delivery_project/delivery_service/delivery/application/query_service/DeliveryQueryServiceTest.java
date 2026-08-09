package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.query.DeliveryGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRouteGetQuery;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteQueryRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryQueryServiceTest {

    @Mock
    private DeliveryQueryRepository deliveryQueryRepository;

    @Mock
    private DeliveryManagerQueryRepository deliveryManagerQueryRepository;

    @Mock
    private DeliveryRouteQueryRepository deliveryRouteQueryRepository;

    @InjectMocks
    private DeliveryQueryService deliveryQueryService;

    @InjectMocks
    private DeliveryRouteQueryService deliveryRouteQueryService;

    @Test
    @DisplayName("MASTER는 배송 단건을 조회할 수 있다")
    void getDeliveryMasterSuccess() {
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryGetQuery query =
                DeliveryGetQuery.from(
                        deliveryId,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        deliveryQueryService.getDelivery(query);

        verify(deliveryQueryRepository)
                .findById(deliveryId);

        verifyNoInteractions(deliveryManagerQueryRepository);
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 본인이 담당하지 않은 배송을 조회할 수 없다")
    void getDeliveryOtherDeliveryManagerForbidden() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager manager = mock(DeliveryManager.class);

        UUID requesterManagerId = UUID.randomUUID();

        DeliveryGetQuery query =
                DeliveryGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.DELIVERY_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerQueryRepository.findByUserId(requesterId))
                .thenReturn(Optional.of(manager));

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(manager.getId())
                .thenReturn(requesterManagerId);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(UUID.randomUUID());

        when(deliveryRouteQueryRepository
                .findAllByDeliveryIdOrderBySequenceAsc(deliveryId))
                .thenReturn(java.util.List.of());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryQueryService.getDelivery(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 본인이 담당하지 않은 배송 경로를 조회할 수 없다")
    void getDeliveryRouteOtherManagerForbidden() {
        UUID routeId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        DeliveryManager manager = mock(DeliveryManager.class);
        Delivery delivery = mock(Delivery.class);

        UUID deliveryId = UUID.randomUUID();

        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        requesterId,
                        Role.DELIVERY_MANAGER
                );

        when(deliveryRouteQueryRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(deliveryManagerQueryRepository.findByUserId(requesterId))
                .thenReturn(Optional.of(manager));

        when(manager.getId())
                .thenReturn(UUID.randomUUID());

        when(route.getDeliveryManagerId())
                .thenReturn(UUID.randomUUID());

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(UUID.randomUUID());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteQueryService
                                .getDeliveryRoute(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN,
                exception.getErrorCode()
        );
    }
}