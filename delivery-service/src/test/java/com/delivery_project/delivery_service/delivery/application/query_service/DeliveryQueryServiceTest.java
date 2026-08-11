package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.port.OrderPort;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryListQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRouteGetQuery;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryListResult;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private UserPort userPort;

    @Mock
    private OrderPort orderPort;

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
                .existsByDeliveryIdAndDeliveryManagerId(
                        deliveryId,
                        requesterManagerId
                ))
                .thenReturn(false);

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

    @Test
    @DisplayName("DELIVERY_MANAGER는 본인이 담당한 Route가 있는 배송을 조회할 수 있다")
    void getDeliveryAssignedRouteManagerSuccess() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager manager = mock(DeliveryManager.class);

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
                .thenReturn(managerId);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(UUID.randomUUID());

        when(deliveryRouteQueryRepository
                .existsByDeliveryIdAndDeliveryManagerId(
                        deliveryId,
                        managerId
                ))
                .thenReturn(true);

        deliveryQueryService.getDelivery(query);

        verify(deliveryRouteQueryRepository)
                .existsByDeliveryIdAndDeliveryManagerId(
                        deliveryId,
                        managerId
                );
    }

    @Test
    @DisplayName("HUB_MANAGER는 담당 허브를 경유하는 배송을 조회할 수 있다")
    void getDeliveryHubManagerRelatedHubSuccess() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryGetQuery query =
                DeliveryGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(deliveryRouteQueryRepository
                .existsByDeliveryIdAndHubId(
                        deliveryId,
                        hubId
                ))
                .thenReturn(true);

        deliveryQueryService.getDelivery(query);

        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(deliveryRouteQueryRepository)
                .existsByDeliveryIdAndHubId(
                        deliveryId,
                        hubId
                );
    }

    @Test
    @DisplayName("HUB_MANAGER는 담당 허브와 관련 없는 배송을 조회할 수 없다")
    void getDeliveryHubManagerUnrelatedHubForbidden() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryGetQuery query =
                DeliveryGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(deliveryRouteQueryRepository
                .existsByDeliveryIdAndHubId(
                        deliveryId,
                        hubId
                ))
                .thenReturn(false);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryQueryService.getDelivery(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(deliveryRouteQueryRepository)
                .existsByDeliveryIdAndHubId(
                        deliveryId,
                        hubId
                );
    }

    @Test
    @DisplayName("HUB_MANAGER의 담당 허브가 없으면 배송을 조회할 수 없다")
    void getDeliveryHubManagerWithoutHubForbidden() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryGetQuery query =
                DeliveryGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                null,
                                null
                        )
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryQueryService.getDelivery(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(deliveryRouteQueryRepository, never())
                .existsByDeliveryIdAndHubId(
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("HUB_MANAGER는 담당 허브 기준으로 배송 목록을 조회할 수 있다")
    void getDeliveriesHubManagerSuccess() {
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryListQuery query = mock(DeliveryListQuery.class);

        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(10);
        when(query.requesterId()).thenReturn(requesterId);
        when(query.requesterRole()).thenReturn(Role.HUB_MANAGER);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(deliveryQueryRepository.search(
                eq(query),
                any(Pageable.class),
                isNull(),
                eq(hubId),
                isNull()
        )).thenReturn(Page.empty());

        deliveryQueryService.getDeliveries(query);

        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(deliveryQueryRepository)
                .search(
                        eq(query),
                        any(Pageable.class),
                        isNull(),
                        eq(hubId),
                        isNull()
                );
    }

    @Test
    @DisplayName("HUB_MANAGER의 담당 허브가 없으면 배송 목록을 조회할 수 없다")
    void getDeliveriesHubManagerWithoutHubForbidden() {
        UUID requesterId = UUID.randomUUID();

        DeliveryListQuery query = mock(DeliveryListQuery.class);

        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(10);
        when(query.requesterId()).thenReturn(requesterId);
        when(query.requesterRole()).thenReturn(Role.HUB_MANAGER);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                null,
                                null
                        )
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryQueryService.getDeliveries(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(deliveryQueryRepository, never())
                .search(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("COMPANY_MANAGER는 소속 업체와 관련된 주문의 배송 목록을 조회할 수 있다")
    void getDeliveriesCompanyManagerSuccess() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        List<UUID> relatedOrderIds =
                List.of(orderId1, orderId2);

        DeliveryListQuery query =
                DeliveryListQuery.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "createdAt",
                        "desc",
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        UserAuthorizationInfo requester =
                new UserAuthorizationInfo(
                        requesterId,
                        null,
                        companyId
                );

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(requester);

        when(orderPort.getRelatedOrderIds(companyId))
                .thenReturn(relatedOrderIds);

        when(deliveryQueryRepository.search(
                eq(query),
                any(Pageable.class),
                isNull(),
                isNull(),
                eq(relatedOrderIds)
        )).thenReturn(Page.empty());

        // when
        Page<DeliveryListResult> result =
                deliveryQueryService.getDeliveries(query);

        // then
        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(orderPort)
                .getRelatedOrderIds(companyId);

        verify(deliveryQueryRepository)
                .search(
                        eq(query),
                        any(Pageable.class),
                        isNull(),
                        isNull(),
                        eq(relatedOrderIds)
                );

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("COMPANY_MANAGER의 소속 업체와 관련된 주문이 없으면 빈 배송 목록을 조회한다")
    void getDeliveriesCompanyManagerNoRelatedOrders() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        DeliveryListQuery query =
                DeliveryListQuery.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "createdAt",
                        "desc",
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        UserAuthorizationInfo requester =
                new UserAuthorizationInfo(
                        requesterId,
                        null,
                        companyId
                );

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(requester);

        when(orderPort.getRelatedOrderIds(companyId))
                .thenReturn(List.of());

        when(deliveryQueryRepository.search(
                eq(query),
                any(Pageable.class),
                isNull(),
                isNull(),
                eq(List.of())
        )).thenReturn(Page.empty());

        // when
        Page<DeliveryListResult> result =
                deliveryQueryService.getDeliveries(query);

        // then
        assertTrue(result.isEmpty());

        verify(orderPort)
                .getRelatedOrderIds(companyId);

        verify(deliveryQueryRepository)
                .search(
                        eq(query),
                        any(Pageable.class),
                        isNull(),
                        isNull(),
                        eq(List.of())
                );
    }

    @Test
    @DisplayName("COMPANY_MANAGER의 companyId가 없으면 배송 목록을 조회할 수 없다")
    void getDeliveriesCompanyManagerWithoutCompanyForbidden() {
        // given
        UUID requesterId = UUID.randomUUID();

        DeliveryListQuery query =
                DeliveryListQuery.of(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "createdAt",
                        "desc",
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        UserAuthorizationInfo requester =
                new UserAuthorizationInfo(
                        requesterId,
                        null,
                        null
                );

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(requester);

        // when & then
        assertThrows(
                BusinessException.class,
                () -> deliveryQueryService.getDeliveries(query)
        );

        verify(orderPort, never())
                .getRelatedOrderIds(any());

        verify(deliveryQueryRepository, never())
                .search(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
}