package com.delivery_project.user_service.user.infrastructure.client.delivery;

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
class DeliveryManagerFeignAdapterTest {

	@Mock
	private DeliveryManagerClient deliveryManagerClient;

	@InjectMocks
	private DeliveryManagerFeignAdapter deliveryManagerFeignAdapter;

	@Test
	void 정상_삭제되면_예외없이_반환한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when & then
		assertThatCode(() -> deliveryManagerFeignAdapter.deleteByUserId(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void 대상_레코드가_없으면_멱등_성공으로_처리한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakeDeleteRequest(userId);
		Mockito.doThrow(new FeignException.NotFound("Not Found", request, null, Collections.emptyMap()))
				.when(deliveryManagerClient).deleteByUserId(userId);

		// when & then
		assertThatCode(() -> deliveryManagerFeignAdapter.deleteByUserId(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void 그_외_실패는_DELIVERY_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakeDeleteRequest(userId);
		Mockito.doThrow(new RetryableException(
						503, "Read timed out", Request.HttpMethod.DELETE, (Long) null, request))
				.when(deliveryManagerClient).deleteByUserId(userId);

		// when & then
		assertThatThrownBy(() -> deliveryManagerFeignAdapter.deleteByUserId(userId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
	}

	private Request fakeDeleteRequest(UUID userId) {
		return Request.create(
				"DELETE", "/internal/v1/delivery-managers/users/" + userId,
				Collections.emptyMap(), null, (Charset) null);
	}
}
