package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerListQuery;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerQueryRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryManagerQueryServiceTest {

    @Mock
    private DeliveryManagerQueryRepository deliveryManagerQueryRepository;

    @Mock
    private UserPort userPort;

    @InjectMocks
    private DeliveryManagerQueryService deliveryManagerQueryService;

    @Test
    @DisplayName("MASTER는 배송 담당자 목록 전체를 조회할 수 있다")
    void getDeliveryManagersMasterSuccess() {
        DeliveryManagerListQuery query =
                new DeliveryManagerListQuery(
                        0,
                        10,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        Page<DeliveryManager> page =
                new PageImpl<>(List.of());

        when(deliveryManagerQueryRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        deliveryManagerQueryService.getDeliveryManagers(query);

        verify(deliveryManagerQueryRepository)
                .findAll(any(Pageable.class));

        verify(deliveryManagerQueryRepository, never())
                .findAllByHubId(
                        any(UUID.class),
                        any(Pageable.class)
                );

        verifyNoInteractions(userPort);
    }

    @Test
    @DisplayName("HUB_MANAGER는 자신의 담당 허브 배송 담당자 목록을 조회할 수 있다")
    void getDeliveryManagersHubManagerSuccess() {
        UUID requesterId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryManagerListQuery query =
                new DeliveryManagerListQuery(
                        0,
                        10,
                        requesterId,
                        Role.HUB_MANAGER
                );

        UserAuthorizationInfo authorizationInfo =
                new UserAuthorizationInfo(
                        requesterId,
                        hubId,
                        null
                );

        Page<DeliveryManager> page =
                new PageImpl<>(List.of());

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(authorizationInfo);

        when(deliveryManagerQueryRepository.findAllByHubId(
                eq(hubId),
                any(Pageable.class)
        )).thenReturn(page);

        deliveryManagerQueryService.getDeliveryManagers(query);

        verify(userPort)
                .getUserAuthorizationInfo(requesterId);

        verify(deliveryManagerQueryRepository)
                .findAllByHubId(
                        eq(hubId),
                        any(Pageable.class)
                );

        verify(deliveryManagerQueryRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("HUB_MANAGER의 담당 허브가 없으면 배송 담당자 목록을 조회할 수 없다")
    void getDeliveryManagersHubManagerWithoutHubForbidden() {
        UUID requesterId = UUID.randomUUID();

        DeliveryManagerListQuery query =
                new DeliveryManagerListQuery(
                        0,
                        10,
                        requesterId,
                        Role.HUB_MANAGER
                );

        UserAuthorizationInfo authorizationInfo =
                new UserAuthorizationInfo(
                        requesterId,
                        null,
                        null
                );

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(authorizationInfo);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryManagerQueryService
                                .getDeliveryManagers(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_MANAGER_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(deliveryManagerQueryRepository, never())
                .findAllByHubId(
                        any(UUID.class),
                        any(Pageable.class)
                );
    }

    @Test
    @DisplayName("HUB_MANAGER는 같은 허브의 배송 담당자를 단건 조회할 수 있다")
    void getDeliveryManagerSameHubSuccess() {
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        DeliveryManager deliveryManager =
                mock(DeliveryManager.class);

        DeliveryManagerGetQuery query =
                new DeliveryManagerGetQuery(
                        managerId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryManagerQueryRepository.findById(managerId))
                .thenReturn(Optional.of(deliveryManager));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                hubId,
                                null
                        )
                );

        when(deliveryManager.getHubId())
                .thenReturn(hubId);

        deliveryManagerQueryService.getDeliveryManager(query);

        verify(userPort)
                .getUserAuthorizationInfo(requesterId);
    }

    @Test
    @DisplayName("HUB_MANAGER는 다른 허브의 배송 담당자를 단건 조회할 수 없다")
    void getDeliveryManagerOtherHubForbidden() {
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        UUID requesterHubId = UUID.randomUUID();
        UUID otherHubId = UUID.randomUUID();

        DeliveryManager deliveryManager =
                mock(DeliveryManager.class);

        DeliveryManagerGetQuery query =
                new DeliveryManagerGetQuery(
                        managerId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryManagerQueryRepository.findById(managerId))
                .thenReturn(Optional.of(deliveryManager));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                requesterHubId,
                                null
                        )
                );

        when(deliveryManager.getHubId())
                .thenReturn(otherHubId);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryManagerQueryService
                                .getDeliveryManager(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_MANAGER_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("HUB_MANAGER는 HUB_DELIVERY 배송 담당자를 단건 조회할 수 없다")
    void getHubDeliveryManagerByHubManagerForbidden() {
        UUID requesterId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID requesterHubId = UUID.randomUUID();

        DeliveryManager deliveryManager =
                mock(DeliveryManager.class);

        DeliveryManagerGetQuery query =
                new DeliveryManagerGetQuery(
                        managerId,
                        requesterId,
                        Role.HUB_MANAGER
                );

        when(deliveryManagerQueryRepository.findById(managerId))
                .thenReturn(Optional.of(deliveryManager));

        when(userPort.getUserAuthorizationInfo(requesterId))
                .thenReturn(
                        new UserAuthorizationInfo(
                                requesterId,
                                requesterHubId,
                                null
                        )
                );

        // HUB_DELIVERY는 특정 허브 소속이 아니므로 hubId가 null
        when(deliveryManager.getHubId())
                .thenReturn(null);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryManagerQueryService
                                .getDeliveryManager(query)
                );

        assertEquals(
                ErrorCode.READ_DELIVERY_MANAGER_FORBIDDEN,
                exception.getErrorCode()
        );
    }
}