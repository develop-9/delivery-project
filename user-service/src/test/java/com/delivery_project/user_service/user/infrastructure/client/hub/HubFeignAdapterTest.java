package com.delivery_project.user_service.user.infrastructure.client.hub;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;

@ExtendWith(MockitoExtension.class)
class HubFeignAdapterTest {

	@Mock
	private HubClient hubClient;

	@InjectMocks
	private HubFeignAdapter hubFeignAdapter;

	@Test
	void 존재하는_허브면_예외없이_반환한다() {
		// given
		UUID hubId = UUID.randomUUID();

		// when & then
		assertThatCode(() -> hubFeignAdapter.validateExists(hubId))
				.doesNotThrowAnyException();
	}

	@Test
	void 존재하지_않는_허브면_HUB_NOT_FOUND로_변환한다() {
		// given
		UUID hubId = UUID.randomUUID();
		Request request = fakeGetRequest(hubId);
		Mockito.doThrow(new FeignException.NotFound("Not Found", request, null, Collections.emptyMap()))
				.when(hubClient).getHub(hubId);

		// when & then
		assertThatThrownBy(() -> hubFeignAdapter.validateExists(hubId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HUB_NOT_FOUND);
	}

	@Test
	void 그_외_실패는_HUB_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		UUID hubId = UUID.randomUUID();
		Request request = fakeGetRequest(hubId);
		Mockito.doThrow(new RetryableException(
						503, "Read timed out", Request.HttpMethod.GET, (Long) null, request))
				.when(hubClient).getHub(hubId);

		// when & then
		assertThatThrownBy(() -> hubFeignAdapter.validateExists(hubId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.HUB_SERVICE_UNAVAILABLE);
	}

	private Request fakeGetRequest(UUID hubId) {
		return Request.create(
				"GET", "/internal/v1/hubs/" + hubId,
				Collections.emptyMap(), null, (Charset) null);
	}
}
