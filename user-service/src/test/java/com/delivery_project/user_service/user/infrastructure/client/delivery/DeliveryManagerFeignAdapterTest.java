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
	void 진행_중인_배송이_있으면_DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY로_변환한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakeDeleteRequest(userId);
		Mockito.doThrow(new FeignException.Conflict("Conflict", request, null, Collections.emptyMap()))
				.when(deliveryManagerClient).deleteByUserId(userId);

		// when & then
		assertThatThrownBy(() -> deliveryManagerFeignAdapter.deleteByUserId(userId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY);
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

	@Test
	void 정상_비활성화되면_예외없이_반환한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when & then
		assertThatCode(() -> deliveryManagerFeignAdapter.deactivate(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void 비활성화_대상_레코드가_없으면_멱등_성공으로_처리한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakePatchRequest(userId, "deactivate");
		Mockito.doThrow(new FeignException.NotFound("Not Found", request, null, Collections.emptyMap()))
				.when(deliveryManagerClient).deactivateByUserId(userId);

		// when & then
		assertThatCode(() -> deliveryManagerFeignAdapter.deactivate(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void 비활성화_그_외_실패는_DELIVERY_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakePatchRequest(userId, "deactivate");
		Mockito.doThrow(new RetryableException(
						503, "Read timed out", Request.HttpMethod.PATCH, (Long) null, request))
				.when(deliveryManagerClient).deactivateByUserId(userId);

		// when & then
		assertThatThrownBy(() -> deliveryManagerFeignAdapter.deactivate(userId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
	}

	@Test
	void 정상_재활성화되면_예외없이_반환한다() {
		// given
		UUID userId = UUID.randomUUID();

		// when & then
		assertThatCode(() -> deliveryManagerFeignAdapter.reactivate(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void 재활성화_대상_레코드가_없으면_멱등_성공으로_처리한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakePatchRequest(userId, "reactivate");
		Mockito.doThrow(new FeignException.NotFound("Not Found", request, null, Collections.emptyMap()))
				.when(deliveryManagerClient).reactivateByUserId(userId);

		// when & then
		assertThatCode(() -> deliveryManagerFeignAdapter.reactivate(userId))
				.doesNotThrowAnyException();
	}

	@Test
	void 재활성화_그_외_실패는_DELIVERY_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		UUID userId = UUID.randomUUID();
		Request request = fakePatchRequest(userId, "reactivate");
		Mockito.doThrow(new RetryableException(
						503, "Read timed out", Request.HttpMethod.PATCH, (Long) null, request))
				.when(deliveryManagerClient).reactivateByUserId(userId);

		// when & then
		assertThatThrownBy(() -> deliveryManagerFeignAdapter.reactivate(userId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
	}

	private Request fakeDeleteRequest(UUID userId) {
		return Request.create(
				"DELETE", "/internal/v1/delivery-managers/users/" + userId,
				Collections.emptyMap(), null, (Charset) null);
	}

	private Request fakePatchRequest(UUID userId, String action) {
		return Request.create(
				"PATCH", "/internal/v1/delivery-managers/users/" + userId + "/" + action,
				Collections.emptyMap(), null, (Charset) null);
	}
}
