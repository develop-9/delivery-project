package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryTest {

    @Test
    @DisplayName("PENDING 상태의 배송은 취소할 수 있다")
    void cancelPendingDelivery() {
        // given
        Delivery delivery = createDelivery();

        assertEquals(
                DeliveryStatus.PENDING,
                delivery.getStatus()
        );

        // when
        delivery.cancel();

        // then
        assertEquals(
                DeliveryStatus.CANCELED,
                delivery.getStatus()
        );
    }

    @Test
    @DisplayName("이미 취소된 배송을 다시 취소하면 예외가 발생한다")
    void cancelAlreadyCanceledDelivery() {
        // given
        Delivery delivery = createDelivery();
        delivery.cancel();

        // when
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        delivery::cancel
                );

        // then
        assertEquals(
                ErrorCode.DELIVERY_ALREADY_CANCELED,
                exception.getErrorCode()
        );
    }

    private Delivery createDelivery() {
        return Delivery.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "서울특별시 중구 테스트로 1",
                "홍길동",
                "slack-test"
        );
    }
}