package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.port.CompanyPort;
import com.delivery_project.order_service.order.application.port.UserPort;
import com.delivery_project.order_service.order.infrastructure.client.CompanyInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.DeliveryInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.UserInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.dto.CompanyInfoResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.DeliveryCreateApiRequest;
import com.delivery_project.order_service.order.infrastructure.client.dto.DeliveryCreateApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.UserInfoResponse;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * 어댑터가 상대 서비스의 실패를 우리 도메인 오류로 옮기는 규칙.
 *
 * <p>실제 HTTP 는 나가지 않는다. Feign 인터페이스가 상대 서비스와 실제로 맞는지는
 * 서비스를 함께 띄운 통합 확인에서만 알 수 있고, 여기서는 <b>응답을 받은 뒤의 판단</b>만 본다.
 * 특히 "200 인데 내용이 비어 있는" 경우를 성공으로 넘기지 않는지가 핵심이다.
 */
@ExtendWith(MockitoExtension.class)
class FeignAdapterTest {

	private static FeignException notFound() {
		return new FeignException.NotFound("not found", request(), null, Map.of());
	}

	private static FeignException serverError() {
		return new FeignException.InternalServerError("boom", request(), null, Map.of());
	}

	private static Request request() {
		return Request.create(Request.HttpMethod.GET, "/internal/v1/x",
				Map.of(), null, StandardCharsets.UTF_8, null);
	}

	@Nested
	@DisplayName("업체 조회")
	class Company {

		@Mock
		private CompanyInternalClient client;

		@InjectMocks
		private CompanyFeignAdapter adapter;

		private final UUID companyId = UUID.randomUUID();

		@Test
		@DisplayName("허브와 주소를 뽑아 온다")
		void success() {
			// given
			UUID hubId = UUID.randomUUID();
			given(client.getCompany(companyId)).willReturn(new InternalApiResponse<>(true,
					new CompanyInfoResponse(companyId, "동보야채", "RECEIVER", hubId, "서울시 송파구")));

			// when
			CompanyPort.ReceiverCompany receiver = adapter.getReceiverCompany(companyId);

			// then
			assertThat(receiver.hubId()).isEqualTo(hubId);
			assertThat(receiver.address()).isEqualTo("서울시 송파구");
		}

		@Test
		@DisplayName("hubId 나 address 가 비어 있으면 배송 생성까지 가지 않고 끊는다")
		void missingFieldsAreRejectedEarly() {
			// given — 상대 응답에 아직 두 필드가 없는 현재 상태
			given(client.getCompany(companyId)).willReturn(new InternalApiResponse<>(true,
					new CompanyInfoResponse(companyId, "동보야채", "RECEIVER", null, null)));

			// when & then — delivery 가 400 을 주면 원인이 company 라는 게 드러나지 않는다
			assertThatThrownBy(() -> adapter.getReceiverCompany(companyId))
					.isInstanceOf(BusinessException.class)
					.hasMessageContaining("허브·주소");
		}

		@Test
		@DisplayName("없는 업체는 404 로 옮긴다")
		void notFoundIsTranslated() {
			// given
			willThrow(notFound()).given(client).getCompany(companyId);

			// when & then
			assertThatThrownBy(() -> adapter.getReceiverCompany(companyId))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode").isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
		}

		@Test
		@DisplayName("서버 오류는 연동 불가로 옮긴다")
		void serverErrorIsTranslated() {
			// given
			willThrow(serverError()).given(client).getCompany(companyId);

			// when & then
			assertThatThrownBy(() -> adapter.getReceiverCompany(companyId))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
		}
	}

	@Nested
	@DisplayName("사용자 조회")
	class User {

		@Mock
		private UserInternalClient client;

		@InjectMocks
		private UserFeignAdapter adapter;

		private final UUID userId = UUID.randomUUID();

		@Test
		@DisplayName("소속 업체까지 함께 가져온다")
		void success() {
			// given
			UUID companyId = UUID.randomUUID();
			given(client.getUser(userId)).willReturn(new InternalApiResponse<>(true,
					new UserInfoResponse(userId, "김수령", "COMPANY_MANAGER", null, companyId)));

			// when
			UserPort.Receiver receiver = adapter.getReceiver(userId);

			// then
			assertThat(receiver.companyId()).isEqualTo(companyId);
		}

		@Test
		@DisplayName("없는 사용자는 404 로 옮긴다")
		void notFoundIsTranslated() {
			// given
			willThrow(notFound()).given(client).getUser(userId);

			// when & then
			assertThatThrownBy(() -> adapter.getReceiver(userId))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
		}

		@Test
		@DisplayName("success=false 면 성공으로 넘기지 않는다")
		void unsuccessfulEnvelopeIsRejected() {
			// given — HTTP 는 200 이지만 봉투가 실패를 말한다
			given(client.getUser(userId)).willReturn(new InternalApiResponse<>(false, null));

			// when & then
			assertThatThrownBy(() -> adapter.getReceiver(userId))
					.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("배송 생성·취소")
	class Delivery {

		@Mock
		private DeliveryInternalClient client;

		@InjectMocks
		private DeliveryFeignAdapter adapter;

		private final UUID orderId = UUID.randomUUID();
		private final UUID departureHubId = UUID.randomUUID();
		private final UUID destinationHubId = UUID.randomUUID();
		private final UUID receiverUserId = UUID.randomUUID();

		private UUID create() {
			return adapter.createDelivery(orderId, departureHubId, destinationHubId,
					"서울시 송파구", receiverUserId);
		}

		@Test
		@DisplayName("delivery 계약대로 필드를 채워 보낸다")
		void sendsContractFields() {
			// given
			UUID deliveryId = UUID.randomUUID();
			given(client.createDelivery(any())).willReturn(
					new InternalApiResponse<>(true, new DeliveryCreateApiResponse(deliveryId)));

			// when
			UUID created = create();

			// then
			assertThat(created).isEqualTo(deliveryId);
			then(client).should().createDelivery(new DeliveryCreateApiRequest(
					orderId, departureHubId, destinationHubId, "서울시 송파구", receiverUserId));
		}

		@Test
		@DisplayName("생성 실패는 DELIVERY_CREATE_FAILED 로 옮긴다")
		void createFailureIsTranslated() {
			// given
			willThrow(serverError()).given(client).createDelivery(any());

			// when & then
			assertThatThrownBy(this::create)
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode").isEqualTo(ErrorCode.DELIVERY_CREATE_FAILED);
		}

		@Test
		@DisplayName("취소할 배송이 없는 것은 오류가 아니다")
		void cancelWithoutDeliveryIsNotAnError() {
			// given — 배송 생성 전에 취소된 주문
			willThrow(notFound()).given(client).cancelDelivery(orderId);

			// when & then — 취소를 막을 이유가 없다
			assertThatCode(() -> adapter.cancelDelivery(orderId)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("취소 실패는 드러내서 재시도할 수 있게 한다")
		void cancelFailureIsTranslated() {
			// given
			willThrow(serverError()).given(client).cancelDelivery(orderId);

			// when & then
			assertThatThrownBy(() -> adapter.cancelDelivery(orderId))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode").isEqualTo(ErrorCode.DELIVERY_CANCEL_FAILED);
		}
	}
}
