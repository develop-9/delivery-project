package com.delivery_project.order_service.order.application.facade;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.command.OrderCreateCommand;
import com.delivery_project.order_service.order.application.command.OrderItemCommand;
import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.port.CompanyPort;
import com.delivery_project.order_service.order.application.port.DeliveryPort;
import com.delivery_project.order_service.order.application.port.UserPort;
import com.delivery_project.order_service.order.application.result.OrderResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 주문 접수·취소 연동 조립 검증.
 *
 * <p>원자성이 없는 구간(주문은 저장됐는데 배송 생성이 실패)에서 어떤 상태로 남는지가
 * 이 클래스의 핵심이라, 실패 경로를 함께 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

	@Mock
	private OrderCommandService orderCommandService;

	@Mock
	private CompanyPort companyPort;

	@Mock
	private DeliveryPort deliveryPort;

	@Mock
	private UserPort userPort;

	@InjectMocks
	private OrderFacade orderFacade;

	private final UUID orderId = UUID.randomUUID();
	private final UUID receiverCompanyId = UUID.randomUUID();
	private final UUID receiverUserId = UUID.randomUUID();
	private final UUID departureHubId = UUID.randomUUID();
	private final UUID destinationHubId = UUID.randomUUID();

	private OrderCreateCommand command() {
		return new OrderCreateCommand(
				receiverUserId, UUID.randomUUID(), receiverCompanyId, "오전 중 배송 부탁드립니다",
				List.of(new OrderItemCommand(UUID.randomUUID(), 50)));
	}

	private OrderResult result(String status) {
		return new OrderResult(orderId, status, UUID.randomUUID(), receiverCompanyId, receiverUserId,
				"오전 중 배송 부탁드립니다", List.of(), Instant.now(), Instant.now());
	}

	/** 수령인이 요청한 수령 업체 소속인 정상 상황 */
	private void givenReceiverBelongsToCompany() {
		given(userPort.getReceiver(receiverUserId))
				.willReturn(new UserPort.Receiver(receiverUserId, "김수령", UUID.randomUUID(), receiverCompanyId));
	}

	@Test
	@DisplayName("주문을 저장하고 배송까지 만들면 주문이 확정된다")
	void createSuccess() {
		// given
		givenReceiverBelongsToCompany();
		given(companyPort.getReceiverCompany(receiverCompanyId))
				.willReturn(new CompanyPort.ReceiverCompany(receiverCompanyId, destinationHubId, "서울시 송파구"));
		given(orderCommandService.create(any())).willReturn(result("PENDING"));
		given(orderCommandService.resolveDepartureHubId(orderId)).willReturn(departureHubId);
		given(orderCommandService.confirm(orderId)).willReturn(result("CONFIRMED"));

		// when
		OrderResult created = orderFacade.create(command());

		// then
		assertThat(created.status()).isEqualTo("CONFIRMED");
		then(deliveryPort).should().createDelivery(
				orderId, departureHubId, destinationHubId, "서울시 송파구", receiverUserId);
		then(orderCommandService).should(never()).markFailed(any());
	}

	@Test
	@DisplayName("수령 업체를 못 가져오면 주문을 만들지 않는다")
	void createStopsBeforeSavingWhenCompanyUnavailable() {
		// given
		givenReceiverBelongsToCompany();
		given(companyPort.getReceiverCompany(receiverCompanyId))
				.willThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> orderFacade.create(command()))
				.isInstanceOf(BusinessException.class);

		// 주문을 만든 뒤에 알았다면 되돌릴 일이 생긴다
		then(orderCommandService).should(never()).create(any());
		then(deliveryPort).should(never()).createDelivery(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("배송 생성이 실패하면 주문을 PENDING 으로 두지 않고 실패 처리한다")
	void createMarksOrderFailedWhenDeliveryFails() {
		// given
		givenReceiverBelongsToCompany();
		given(companyPort.getReceiverCompany(receiverCompanyId))
				.willReturn(new CompanyPort.ReceiverCompany(receiverCompanyId, destinationHubId, "서울시 송파구"));
		given(orderCommandService.create(any())).willReturn(result("PENDING"));
		given(orderCommandService.resolveDepartureHubId(orderId)).willReturn(departureHubId);
		given(deliveryPort.createDelivery(any(), any(), any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.DELIVERY_CREATE_FAILED));

		// when & then
		assertThatThrownBy(() -> orderFacade.create(command()))
				.isInstanceOf(BusinessException.class);

		// 배송 없는 주문이 PENDING 으로 남으면 재고만 선점된 채 방치된다
		then(orderCommandService).should().markFailed(orderId);
		then(orderCommandService).should(never()).confirm(any());
	}

	@Test
	@DisplayName("소속과 다른 업체로 주문하면 막고 주문을 만들지 않는다")
	void createRejectsOrderForAnotherCompany() {
		// given — 수령인은 다른 업체 소속이다
		given(userPort.getReceiver(receiverUserId))
				.willReturn(new UserPort.Receiver(receiverUserId, "김수령", UUID.randomUUID(), UUID.randomUUID()));

		// when & then
		assertThatThrownBy(() -> orderFacade.create(command()))
				.isInstanceOf(BusinessException.class);

		then(companyPort).shouldHaveNoInteractions();
		then(orderCommandService).should(never()).create(any());
	}

	@Test
	@DisplayName("소속 업체가 없는 사용자(MASTER 등)는 업체 일치 검사를 건너뛴다")
	void createAllowsUserWithoutCompany() {
		// given
		given(userPort.getReceiver(receiverUserId))
				.willReturn(new UserPort.Receiver(receiverUserId, "관리자", null, null));
		given(companyPort.getReceiverCompany(receiverCompanyId))
				.willReturn(new CompanyPort.ReceiverCompany(receiverCompanyId, destinationHubId, "서울시 송파구"));
		given(orderCommandService.create(any())).willReturn(result("PENDING"));
		given(orderCommandService.resolveDepartureHubId(orderId)).willReturn(departureHubId);
		given(orderCommandService.confirm(orderId)).willReturn(result("CONFIRMED"));

		// when
		OrderResult created = orderFacade.create(command());

		// then
		assertThat(created.status()).isEqualTo("CONFIRMED");
	}

	@Test
	@DisplayName("주문을 먼저 취소한 뒤 배송을 취소한다")
	void cancelOrderBeforeDelivery() {
		// given
		given(orderCommandService.cancel(orderId)).willReturn(result("CANCELED"));

		// when
		OrderResult canceled = orderFacade.cancel(orderId);

		// then
		assertThat(canceled.status()).isEqualTo("CANCELED");
		then(orderCommandService).should().cancel(orderId);
		then(deliveryPort).should().cancelDelivery(orderId);
	}
}
