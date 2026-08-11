package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.port.OrderPort;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRouteGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRoutesGetQuery;
import com.delivery_project.delivery_service.delivery.application.result.OrderCompanyInfo;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteQueryServiceTest {

    @Mock
    private DeliveryQueryRepository deliveryQueryRepository;

    @Mock
    private DeliveryManagerQueryRepository deliveryManagerQueryRepository;

    @Mock
    private DeliveryRouteQueryRepository deliveryRouteQueryRepository;

    @Mock
    private UserPort userPort;

    @Mock
    private OrderPort orderPort;

    @InjectMocks
    private DeliveryRouteQueryService deliveryRouteQueryService;

    @Test
    @DisplayName("HUB_MANAGER는 담당 허브와 연결된 배송 경로를 단건 조회할 수 있다")
    void getDeliveryRouteHubManagerRelatedRouteSuccess() {
        UUID routeId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);

        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryRouteQueryRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(route.getDepartureHubId())
                .thenReturn(hubId);

        when(route.getArrivalHubId())
                .thenReturn(UUID.randomUUID());

        deliveryRouteQueryService.getDeliveryRoute(query);

        verify(userPort)
                .getUserAuthorizationInfo(requesterId);
    }

    @Test
    @DisplayName("HUB_MANAGER는 담당 허브와 관련 없는 배송 경로를 단건 조회할 수 없다")
    void getDeliveryRouteHubManagerUnrelatedRouteForbidden() {
        UUID routeId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);

        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryRouteQueryRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(route.getDepartureHubId())
                .thenReturn(UUID.randomUUID());

        when(route.getArrivalHubId())
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
    @DisplayName("HUB_MANAGER는 담당 허브와 관련된 배송의 전체 경로를 조회할 수 있다")
    void getDeliveryRoutesHubManagerRelatedDeliverySuccess() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryRoute route = mock(DeliveryRoute.class);

        DeliveryRoutesGetQuery query =
                DeliveryRoutesGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(deliveryRouteQueryRepository
                .existsByDeliveryIdAndHubId(
                        deliveryId,
                        hubId
                ))
                .thenReturn(true);

        when(deliveryRouteQueryRepository
                .findAllByDeliveryIdOrderBySequenceAsc(
                        deliveryId
                ))
                .thenReturn(List.of(route));

        deliveryRouteQueryService.getDeliveryRoutes(query);

        verify(deliveryRouteQueryRepository)
                .findAllByDeliveryIdOrderBySequenceAsc(
                        deliveryId
                );
    }

    @Test
    @DisplayName("HUB_MANAGER는 담당 허브와 관련 없는 배송의 전체 경로를 조회할 수 없다")
    void getDeliveryRoutesHubManagerUnrelatedDeliveryForbidden() {
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryRoutesGetQuery query =
                DeliveryRoutesGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(deliveryRouteQueryRepository
                .existsByDeliveryIdAndHubId(
                        deliveryId,
                        hubId
                ))
                .thenReturn(false);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryRouteQueryService
                                .getDeliveryRoutes(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(deliveryRouteQueryRepository, never())
                .findAllByDeliveryIdOrderBySequenceAsc(
                        deliveryId
                );
    }

    @Test
    @DisplayName("COMPANY_MANAGER는 공급 업체와 관련된 배송 경로를 단건 조회할 수 있다")
    void getDeliveryRouteCompanyManagerSupplierSuccess() {
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        Delivery delivery = mock(Delivery.class);

        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        when(deliveryRouteQueryRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getOrderId())
                .thenReturn(orderId);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                null,
                                companyId
                        )
                );

        when(orderPort.getOrderCompanyInfo(orderId))
                .thenReturn(
                        new OrderCompanyInfo(
                                orderId,
                                companyId,
                                UUID.randomUUID()
                        )
                );

        deliveryRouteQueryService.getDeliveryRoute(query);

        verify(orderPort)
                .getOrderCompanyInfo(orderId);
    }

    @Test
    @DisplayName("COMPANY_MANAGER는 수령 업체와 관련된 배송 경로를 단건 조회할 수 있다")
    void getDeliveryRouteCompanyManagerReceiverSuccess() {
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        Delivery delivery = mock(Delivery.class);

        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        when(deliveryRouteQueryRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getOrderId())
                .thenReturn(orderId);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                null,
                                companyId
                        )
                );

        when(orderPort.getOrderCompanyInfo(orderId))
                .thenReturn(
                        new OrderCompanyInfo(
                                orderId,
                                UUID.randomUUID(),
                                companyId
                        )
                );

        deliveryRouteQueryService.getDeliveryRoute(query);

        verify(orderPort)
                .getOrderCompanyInfo(orderId);
    }

    @Test
    @DisplayName("COMPANY_MANAGER는 관련 없는 업체의 배송 경로를 단건 조회할 수 없다")
    void getDeliveryRouteCompanyManagerUnrelatedForbidden() {
        UUID routeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        DeliveryRoute route = mock(DeliveryRoute.class);
        Delivery delivery = mock(Delivery.class);

        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        when(deliveryRouteQueryRepository.findById(routeId))
                .thenReturn(Optional.of(route));

        when(route.getDeliveryId())
                .thenReturn(deliveryId);

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getOrderId())
                .thenReturn(orderId);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                null,
                                companyId
                        )
                );

        when(orderPort.getOrderCompanyInfo(orderId))
                .thenReturn(
                        new OrderCompanyInfo(
                                orderId,
                                UUID.randomUUID(),
                                UUID.randomUUID()
                        )
                );

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
    @DisplayName("COMPANY_MANAGER는 관련 업체 배송의 전체 경로를 조회할 수 있다")
    void getDeliveryRoutesCompanyManagerSuccess() {
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryRoute route = mock(DeliveryRoute.class);

        DeliveryRoutesGetQuery query =
                DeliveryRoutesGetQuery.from(
                        deliveryId,
                        requesterId,
                        Role.COMPANY_MANAGER
                );

        when(deliveryQueryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(delivery.getOrderId())
                .thenReturn(orderId);

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                null,
                                companyId
                        )
                );

        when(orderPort.getOrderCompanyInfo(orderId))
                .thenReturn(
                        new OrderCompanyInfo(
                                orderId,
                                companyId,
                                UUID.randomUUID()
                        )
                );

        when(deliveryRouteQueryRepository
                .findAllByDeliveryIdOrderBySequenceAsc(deliveryId))
                .thenReturn(List.of(route));

        deliveryRouteQueryService.getDeliveryRoutes(query);

        verify(deliveryRouteQueryRepository)
                .findAllByDeliveryIdOrderBySequenceAsc(
                        deliveryId
                );
    }
}