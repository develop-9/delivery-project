package com.delivery_project.order_service.order.application.facade;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.command.OrderCreateCommand;
import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.port.CompanyPort;
import com.delivery_project.order_service.order.application.port.DeliveryPort;
import com.delivery_project.order_service.order.application.port.UserPort;
import com.delivery_project.order_service.order.application.result.OrderResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 주문 접수·취소의 <b>서비스 간 연동</b>을 조립한다.
 *
 * <p>이 클래스에 {@code @Transactional} 을 붙이지 않는 것이 핵심이다.
 * 외부 호출은 응답이 늦거나 영영 안 올 수 있는데, 트랜잭션 안에서 부르면 DB 커넥션을 쥔 채
 * 네트워크를 기다리게 되어 부하가 몰릴 때 커넥션 풀이 마른다.
 * DB 작업은 {@link OrderCommandService} 의 짧은 트랜잭션으로 나눠 넣고,
 * 그 사이사이에 외부 호출을 둔다.
 *
 * <p>대신 원자성이 사라진다. 주문은 저장됐는데 배송 생성이 실패하는 구간이 생기므로,
 * 그 경우 주문을 {@code FAILED} 로 내려 <b>배송 없는 주문이 PENDING 으로 남지 않게</b> 한다.
 * 상태 기계상 {@code FAILED} 는 종착역이라 이후 다시 진행되지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

	private final OrderCommandService orderCommandService;
	private final CompanyPort companyPort;
	private final DeliveryPort deliveryPort;
	private final UserPort userPort;

	/**
	 * 주문을 접수하고 배송까지 만든다.
	 *
	 * <pre>
	 * 0. 수령인 검증         (트랜잭션 밖)  ← 소속 업체가 다르면 여기서 끊는다
	 * 1. 수령 업체 조회      (트랜잭션 밖)  ← 없는 업체면 주문을 만들기 전에 멈춘다
	 * 2. 주문 저장           (트랜잭션 1)   PENDING · 재고 선점 · 스냅샷
	 * 3. 배송 생성 호출      (트랜잭션 밖)
	 * 4. 주문 확정 / 실패     (트랜잭션 2)   CONFIRMED · FAILED + 스냅샷
	 * </pre>
	 *
	 * <p>검증(0·1)을 저장(2)보다 앞에 두는 것이 규칙이다. 저장 뒤에 알게 되면 되돌려야 하는데,
	 * 되돌리는 코드는 실패했을 때 확인할 방법이 마땅치 않다.
	 */
	public OrderResult create(OrderCreateCommand command) {
		// 0. 수령인이 실존하는지, 그 사람이 그 업체 소속인지 확인한다
		validateReceiver(command);

		// 1. 도착 허브와 배송지를 먼저 확보한다. 주문을 만든 뒤에 알면 되돌릴 일이 생긴다
		CompanyPort.ReceiverCompany receiver =
				companyPort.getReceiverCompany(command.receiverCompanyId());

		// 2.
		OrderResult order = orderCommandService.create(command);
		UUID departureHubId = orderCommandService.resolveDepartureHubId(order.orderId());

		// 3.
		try {
			deliveryPort.createDelivery(
					order.orderId(),
					departureHubId,
					receiver.hubId(),
					receiver.address(),
					command.receiverUserId());

		} catch (BusinessException exception) {
			// 4b. 주문만 남기고 끝내지 않는다. 선점한 재고를 여기서 되돌려야 한다
			log.error("[주문] 배송 생성 실패로 주문 실패 처리 : [{}] departureHubId={}",
					order.orderId(), departureHubId, exception);
			markFailedQuietly(order.orderId());
			throw exception;
		}

		// 4a.
		return orderCommandService.confirm(order.orderId());
	}

	/**
	 * 수령인 검증.
	 *
	 * <p>수령인은 인증 주체와 같은 사람이라 "존재하는가"만 보면 사실상 늘 통과한다.
	 * 실제로 막아야 하는 것은 <b>남의 업체 앞으로 주문을 넣는 것</b>이다.
	 * 물건이 도착할 곳이 곧 수령인이 속한 업체이므로 둘은 같아야 한다.
	 *
	 * <p>MASTER·HUB_MANAGER 는 소속 업체가 없어({@code companyId == null}) 이 검사를 건너뛴다.
	 * 역할별 세부 권한은 팀에서 담당 업체·허브 식별 방식이 정해진 뒤에 붙인다.
	 */
	private void validateReceiver(OrderCreateCommand command) {
		UserPort.Receiver receiver = userPort.getReceiver(command.receiverUserId());

		if (receiver.companyId() == null) {
			log.info("[주문] 소속 업체가 없는 사용자의 주문 : receiverUserId={}", receiver.userId());
			return;
		}

		if (!receiver.companyId().equals(command.receiverCompanyId())) {
			log.warn("[주문] 소속과 다른 업체로 주문 시도 : receiverUserId={} 소속={} 요청={}",
					receiver.userId(), receiver.companyId(), command.receiverCompanyId());
			throw new BusinessException(ErrorCode.RECEIVER_COMPANY_MISMATCH);
		}
	}

	/**
	 * 보상 처리는 실패해도 원래 예외를 덮지 않는다.
	 *
	 * <p>여기서 예외가 새어 나가면 호출자는 "주문 실패 처리에 실패했다"만 보게 되고
	 * 정작 원인인 배송 생성 실패가 가려진다. 선점이 남는 것은 로그로 드러낸다.
	 */
	private void markFailedQuietly(UUID orderId) {
		try {
			orderCommandService.markFailed(orderId);
		} catch (RuntimeException exception) {
			log.error("[주문] 실패 처리 자체가 실패 — 선점 재고가 남아 있을 수 있다 : [{}]",
					orderId, exception);
		}
	}

	/**
	 * 주문을 취소하고 배송도 취소한다.
	 *
	 * <p>주문을 먼저 취소한다. 배송 취소가 실패해도 주문은 취소된 상태로 남는데,
	 * 반대 순서(배송 먼저)로 하면 배송만 사라지고 주문은 살아 있는 더 나쁜 어긋남이 생긴다.
	 * 배송 쪽 실패는 예외로 드러내 재시도할 수 있게 둔다.
	 */
	public OrderResult cancel(UUID orderId) {
		OrderResult canceled = orderCommandService.cancel(orderId);

		deliveryPort.cancelDelivery(orderId);

		return canceled;
	}
}
