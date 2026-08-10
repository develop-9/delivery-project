package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.*;
import com.delivery_project.delivery_service.delivery.application.persistence_service.DeliveryPersistenceService;
import com.delivery_project.delivery_service.delivery.application.port.HubRoutePort;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryCreateResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryCommandServiceTest {

    @Mock
    private DeliveryCommandRepository deliveryCommandRepository;

    @Mock
    private DeliveryRouteCommandRepository deliveryRouteCommandRepository;

    @Mock
    private DeliveryManagerCommandRepository deliveryManagerCommandRepository;

    @Mock
    private UserPort userPort;

    @Mock
    private HubRoutePort hubRoutePort;

    @Mock
    private DeliveryPersistenceService deliveryPersistenceService;

    @InjectMocks
    private DeliveryCommandService deliveryCommandService;

    @Test
    @DisplayName("배송 생성 시 User/Hub 정보를 조회한 뒤 저장을 요청한다")
    void createDeliverySuccess() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID departureHubId = UUID.randomUUID();
        UUID destinationHubId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        DeliveryCreateCommand command =
                new DeliveryCreateCommand(
                        orderId,
                        departureHubId,
                        destinationHubId,
                        "부산광역시 사하구 낙동대로 1번길 1",
                        receiverUserId
                );

        ReceiverInfo receiverInfo =
                mock(ReceiverInfo.class);

        DeliveryPath deliveryPath =
                mock(DeliveryPath.class);

        DeliveryCreateResult expectedResult =
                new DeliveryCreateResult(
                        deliveryId,
                        orderId,
                        DeliveryStatus.PENDING,
                        2
                );

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.empty());

        when(userPort.getReceiver(receiverUserId))
                .thenReturn(receiverInfo);

        when(hubRoutePort.getDeliveryPath(
                departureHubId,
                destinationHubId
        )).thenReturn(deliveryPath);

        when(deliveryPersistenceService.create(
                command,
                receiverInfo,
                deliveryPath
        )).thenReturn(expectedResult);

        // when
        DeliveryCreateResult result =
                deliveryCommandService.create(command);

        // then
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

        verify(userPort)
                .getReceiver(receiverUserId);

        verify(hubRoutePort)
                .getDeliveryPath(
                        departureHubId,
                        destinationHubId
                );

        verify(deliveryPersistenceService)
                .create(
                        command,
                        receiverInfo,
                        deliveryPath
                );
    }

    @Test
    @DisplayName("동일 orderId의 배송이 존재하면 중복 생성할 수 없다")
    void createDeliveryDuplicateOrder() {
        // given
        UUID orderId = UUID.randomUUID();

        DeliveryCreateCommand command =
                new DeliveryCreateCommand(
                        orderId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "서울특별시 중구 테스트로 1",
                        UUID.randomUUID()
                );

        Delivery existingDelivery =
                mock(Delivery.class);

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.of(existingDelivery));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.create(command)
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_ALREADY_EXISTS,
                exception.getErrorCode()
        );

        // 중복 배송이면 외부 서비스 호출 및 저장을 진행하지 않는다.
        verifyNoInteractions(userPort);
        verifyNoInteractions(hubRoutePort);
        verifyNoInteractions(deliveryPersistenceService);
    }

    @Test
    @DisplayName("PENDING 배송을 취소하면 CANCELED 상태가 된다")
    void cancelPendingDeliverySuccess() {
        // given
        UUID orderId = UUID.randomUUID();

        Delivery delivery =
                Delivery.create(
                        orderId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "부산광역시 사하구 낙동대로 1번길 1",
                        "홍길동",
                        "slack-test"
                );

        DeliveryCancelCommand command =
                new DeliveryCancelCommand(orderId);

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.of(delivery));

        /*
         * Delivery.create()만 수행한 상태라 ID가 아직 null일 수 있다.
         * 현재 테스트에서는 IN_TRANSIT Route가 없는 상황으로 가정한다.
         */
        when(deliveryRouteCommandRepository
                .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                        delivery.getId(),
                        DeliveryRouteStatus.IN_TRANSIT
                ))
                .thenReturn(List.of());

        when(deliveryCommandRepository.save(delivery))
                .thenReturn(delivery);

        // when
        deliveryCommandService.cancel(command);

        // then
        assertEquals(
                DeliveryStatus.CANCELED,
                delivery.getStatus()
        );

        verify(deliveryCommandRepository)
                .save(delivery);
    }

    @Test
    @DisplayName("존재하지 않는 배송은 취소할 수 없다")
    void cancelDeliveryNotFound() {
        // given
        UUID orderId = UUID.randomUUID();

        DeliveryCancelCommand command =
                new DeliveryCancelCommand(orderId);

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.cancel(command)
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("업체 배송 담당자가 배정된 배송을 취소하면 담당자를 해제한다")
    void cancelDeliveryReleaseCompanyManagerSuccess() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager deliveryManager = mock(DeliveryManager.class);

        DeliveryCancelCommand command =
                new DeliveryCancelCommand(orderId);

        when(delivery.getId())
                .thenReturn(deliveryId);

        when(delivery.getOrderId())
                .thenReturn(orderId);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(managerId);

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository
                .findById(managerId))
                .thenReturn(Optional.of(deliveryManager));

        when(deliveryRouteCommandRepository
                .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                        deliveryId,
                        DeliveryRouteStatus.IN_TRANSIT
                ))
                .thenReturn(List.of());

        when(deliveryCommandRepository.save(delivery))
                .thenReturn(delivery);

        // when
        deliveryCommandService.cancel(command);

        // then
        verify(delivery).cancel();

        verify(deliveryManager)
                .releaseFromDelivery();

        verify(deliveryCommandRepository)
                .save(delivery);
    }

    @Test
    @DisplayName("배정된 업체 배송 담당자를 찾을 수 없으면 배송 취소에 실패한다")
    void cancelDeliveryCompanyManagerNotFound() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryCancelCommand command =
                new DeliveryCancelCommand(orderId);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(managerId);

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository
                .findById(managerId))
                .thenReturn(Optional.empty());

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.cancel(command)
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_MANAGER_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(delivery).cancel();

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("배정된 업체 배송 담당자가 DELIVERING 상태가 아니면 배송 취소에 실패한다")
    void cancelDeliveryCompanyManagerNotDelivering() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryManager deliveryManager =
                DeliveryManager.create(
                        UUID.randomUUID(),
                        hubId,
                        DeliveryManagerType.COMPANY_DELIVERY,
                        0
                );

        /*
         * DeliveryManager.create()의 초기 상태는 AVAILABLE이다.
         * 따라서 releaseFromDelivery() 호출 시
         * DELIVERY_MANAGER_NOT_DELIVERING 예외가 발생한다.
         */

        DeliveryCancelCommand command =
                new DeliveryCancelCommand(orderId);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(managerId);

        when(deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository
                .findById(managerId))
                .thenReturn(Optional.of(deliveryManager));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.cancel(command)
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_MANAGER_NOT_DELIVERING,
                exception.getErrorCode()
        );

        verify(delivery).cancel();

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("업체 배송 시작 시 Delivery가 HUB_ARRIVED에서 DELIVERING으로 변경된다")
    void startCompanyDeliverySuccess() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager manager = mock(DeliveryManager.class);

        DeliveryStatusUpdateCommand command =
                new DeliveryStatusUpdateCommand(
                        deliveryId,
                        DeliveryStatus.DELIVERING,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(delivery.getStatus())
                .thenReturn(
                        DeliveryStatus.HUB_ARRIVED,
                        DeliveryStatus.DELIVERING
                );

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(managerId);

        when(manager.getStatus())
                .thenReturn(DeliveryManagerStatus.DELIVERING);

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(deliveryCommandRepository.save(delivery))
                .thenReturn(delivery);

        // when
        DeliveryStatusUpdateResult result =
                deliveryCommandService.updateStatus(command);

        // then
        verify(delivery).startCompanyDelivery();
        verify(deliveryCommandRepository).save(delivery);

        assertEquals(
                DeliveryStatus.HUB_ARRIVED,
                result.previousStatus()
        );

        assertEquals(
                DeliveryStatus.DELIVERING,
                result.status()
        );
    }

    @Test
    @DisplayName("업체 배송 완료 시 Delivery는 COMPLETED가 되고 담당자는 AVAILABLE로 복원된다")
    void completeCompanyDeliverySuccess() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager manager = mock(DeliveryManager.class);

        DeliveryStatusUpdateCommand command =
                new DeliveryStatusUpdateCommand(
                        deliveryId,
                        DeliveryStatus.COMPLETED,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(delivery.getStatus())
                .thenReturn(
                        DeliveryStatus.DELIVERING,
                        DeliveryStatus.COMPLETED
                );

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(managerId);

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository.findById(managerId))
                .thenReturn(Optional.of(manager));

        when(deliveryCommandRepository.save(delivery))
                .thenReturn(delivery);

        // when
        DeliveryStatusUpdateResult result =
                deliveryCommandService.updateStatus(command);

        // then
        verify(delivery).complete();

        verify(manager)
                .releaseFromDelivery();

        verify(deliveryManagerCommandRepository)
                .save(manager);

        assertEquals(
                DeliveryStatus.DELIVERING,
                result.previousStatus()
        );

        assertEquals(
                DeliveryStatus.COMPLETED,
                result.status()
        );
    }

    @Test
    @DisplayName("업체 배송 담당자가 배정되지 않은 배송은 업체 배송을 시작할 수 없다")
    void startCompanyDeliveryManagerNotAssigned() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryStatusUpdateCommand command =
                new DeliveryStatusUpdateCommand(
                        deliveryId,
                        DeliveryStatus.DELIVERING,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(delivery.getStatus())
                .thenReturn(DeliveryStatus.HUB_ARRIVED);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(null);

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED,
                exception.getErrorCode()
        );

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("배정된 업체 배송 담당자가 DELIVERING 상태가 아니면 업체 배송을 시작할 수 없다")
    void startCompanyDeliveryManagerNotDelivering() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager manager = mock(DeliveryManager.class);

        DeliveryStatusUpdateCommand command =
                new DeliveryStatusUpdateCommand(
                        deliveryId,
                        DeliveryStatus.DELIVERING,
                        UUID.randomUUID(),
                        Role.MASTER
                );

        when(delivery.getStatus())
                .thenReturn(DeliveryStatus.HUB_ARRIVED);

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(managerId);

        when(manager.getStatus())
                .thenReturn(DeliveryManagerStatus.AVAILABLE);

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository.findById(managerId))
                .thenReturn(Optional.of(manager));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_MANAGER_NOT_DELIVERING,
                exception.getErrorCode()
        );

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("COMPANY_MANAGER는 배송 상태를 변경할 수 없다")
    void updateDeliveryStatusCompanyManagerForbidden() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryStatusUpdateCommand command =
                new DeliveryStatusUpdateCommand(
                        deliveryId,
                        DeliveryStatus.DELIVERING,
                        UUID.randomUUID(),
                        Role.COMPANY_MANAGER
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_STATUS_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 본인이 담당하지 않은 배송 상태를 변경할 수 없다")
    void updateDeliveryStatusOtherManagerForbidden() {
        // given
        UUID deliveryId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);
        DeliveryManager requesterManager = mock(DeliveryManager.class);

        DeliveryStatusUpdateCommand command =
                new DeliveryStatusUpdateCommand(
                        deliveryId,
                        DeliveryStatus.DELIVERING,
                        requesterId,
                        Role.DELIVERY_MANAGER
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        when(deliveryManagerCommandRepository.findByUserId(requesterId))
                .thenReturn(Optional.of(requesterManager));

        when(requesterManager.getId())
                .thenReturn(UUID.randomUUID());

        when(delivery.getCompanyDeliveryManagerId())
                .thenReturn(UUID.randomUUID());

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.updateStatus(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_STATUS_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 배송 정보를 수정할 수 없다")
    void updateDeliveryDeliveryManagerForbidden() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryUpdateCommand command =
                new DeliveryUpdateCommand(
                        deliveryId,
                        "변경된 주소",
                        null,
                        UUID.randomUUID(),
                        Role.DELIVERY_MANAGER
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.update(command)
                );

        // then
        assertEquals(
                ErrorCode.UPDATE_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(delivery, never())
                .update(any(), any(), any());

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("COMPANY_MANAGER는 배송을 삭제할 수 없다")
    void deleteDeliveryCompanyManagerForbidden() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryDeleteCommand command =
                new DeliveryDeleteCommand(
                        deliveryId,
                        UUID.randomUUID(),
                        Role.COMPANY_MANAGER
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.delete(command)
                );

        // then
        assertEquals(
                ErrorCode.DELETE_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(delivery, never())
                .validateDeletable();

        verify(deliveryCommandRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("DELIVERY_MANAGER는 배송을 삭제할 수 없다")
    void deleteDeliveryDeliveryManagerForbidden() {
        // given
        UUID deliveryId = UUID.randomUUID();

        Delivery delivery = mock(Delivery.class);

        DeliveryDeleteCommand command =
                new DeliveryDeleteCommand(
                        deliveryId,
                        UUID.randomUUID(),
                        Role.DELIVERY_MANAGER
                );

        when(deliveryCommandRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery));

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deliveryCommandService.delete(command)
                );

        // then
        assertEquals(
                ErrorCode.DELETE_DELIVERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verify(delivery, never())
                .validateDeletable();
    }
}