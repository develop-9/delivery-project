package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.infrastructure.client.UserInternalClient;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.UserInfoResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.UserSlackResponse;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFeignAdapterTest {

    @Mock
    private UserInternalClient userInternalClient;

    @InjectMocks
    private UserFeignAdapter userFeignAdapter;

    @Test
    @DisplayName("사용자 정보와 Slack 정보를 조회하여 수령인 정보로 변환한다")
    void getReceiverSuccess() {
        UUID userId = UUID.randomUUID();

        UserInfoResponse userResponse =
                new UserInfoResponse(
                        userId,
                        "receiver01",
                        "수령인",
                        "COMPANY_MANAGER",
                        null,
                        UUID.randomUUID()
                );

        UserSlackResponse slackResponse =
                new UserSlackResponse(
                        userId,
                        "U12345678"
                );

        when(userInternalClient.getUser(userId))
                .thenReturn(
                        new InternalApiResponse<>(
                                true,
                                userResponse
                        )
                );

        when(userInternalClient.getUserSlack(userId))
                .thenReturn(
                        new InternalApiResponse<>(
                                true,
                                slackResponse
                        )
                );

        ReceiverInfo result =
                userFeignAdapter.getReceiver(userId);

        assertEquals(userId, result.userId());
        assertEquals("수령인", result.name());
        assertEquals("U12345678", result.slackId());

        verify(userInternalClient)
                .getUser(userId);

        verify(userInternalClient)
                .getUserSlack(userId);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 USER_NOT_FOUND 예외로 변환한다")
    void getReceiverUserNotFound() {
        UUID userId = UUID.randomUUID();

        FeignException.NotFound exception =
                mock(FeignException.NotFound.class);

        when(userInternalClient.getUser(userId))
                .thenThrow(exception);

        BusinessException businessException =
                assertThrows(
                        BusinessException.class,
                        () -> userFeignAdapter.getReceiver(userId)
                );

        assertEquals(
                ErrorCode.USER_NOT_FOUND,
                businessException.getErrorCode()
        );

        verify(userInternalClient, never())
                .getUserSlack(any());
    }

    @Test
    @DisplayName("User Service 호출에 실패하면 USER_SERVICE_UNAVAILABLE 예외로 변환한다")
    void getReceiverUserServiceUnavailable() {
        UUID userId = UUID.randomUUID();

        FeignException exception =
                mock(FeignException.class);

        when(userInternalClient.getUser(userId))
                .thenThrow(exception);

        BusinessException businessException =
                assertThrows(
                        BusinessException.class,
                        () -> userFeignAdapter.getReceiver(userId)
                );

        assertEquals(
                ErrorCode.USER_SERVICE_UNAVAILABLE,
                businessException.getErrorCode()
        );
    }
}