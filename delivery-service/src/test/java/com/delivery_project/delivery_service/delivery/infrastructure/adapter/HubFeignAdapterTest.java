package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.infrastructure.client.HubInternalClient;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.HubSummaryResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
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
class HubFeignAdapterTest {

    @Mock
    private HubInternalClient hubInternalClient;

    @InjectMocks
    private HubFeignAdapter hubFeignAdapter;

    @Test
    @DisplayName("허브가 존재하면 정상적으로 검증을 통과한다")
    void validateHubExistsSuccess() {
        UUID hubId = UUID.randomUUID();

        HubSummaryResponse hubResponse =
                new HubSummaryResponse(hubId);

        when(hubInternalClient.getHub(hubId))
                .thenReturn(
                        new InternalApiResponse<>(
                                true,
                                hubResponse
                        )
                );

        hubFeignAdapter.validateHubExists(hubId);

        verify(hubInternalClient)
                .getHub(hubId);
    }

    @Test
    @DisplayName("허브를 찾을 수 없으면 HUB_NOT_FOUND 예외로 변환한다")
    void validateHubExistsNotFound() {
        UUID hubId = UUID.randomUUID();

        FeignException.NotFound exception =
                mock(FeignException.NotFound.class);

        when(hubInternalClient.getHub(hubId))
                .thenThrow(exception);

        BusinessException businessException =
                assertThrows(
                        BusinessException.class,
                        () -> hubFeignAdapter.validateHubExists(hubId)
                );

        assertEquals(
                ErrorCode.HUB_NOT_FOUND,
                businessException.getErrorCode()
        );

        verify(hubInternalClient)
                .getHub(hubId);
    }

    @Test
    @DisplayName("Hub Service 호출에 실패하면 HUB_SERVICE_UNAVAILABLE 예외로 변환한다")
    void validateHubExistsServiceUnavailable() {
        UUID hubId = UUID.randomUUID();

        FeignException exception =
                mock(FeignException.class);

        when(hubInternalClient.getHub(hubId))
                .thenThrow(exception);

        BusinessException businessException =
                assertThrows(
                        BusinessException.class,
                        () -> hubFeignAdapter.validateHubExists(hubId)
                );

        assertEquals(
                ErrorCode.HUB_SERVICE_UNAVAILABLE,
                businessException.getErrorCode()
        );

        verify(hubInternalClient)
                .getHub(hubId);
    }
}