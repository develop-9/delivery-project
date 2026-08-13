package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.order.application.authorization.OrderAccessPolicy;
import com.delivery_project.order_service.order.application.port.CompanyPort;
import com.delivery_project.order_service.order.application.port.ProductPort;
import com.delivery_project.order_service.order.application.port.UserPort;
import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.repository.InventoryQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * getOrderForInternal의 확장 필드 조회(상품명·업체명·허브ID·요청자명) 동작을 검증한다.
 * Slack/AI/Delivery Service가 이 계약을 그대로 믿고 프롬프트/메시지를 만들기 때문에,
 * 확장 조회 하나가 실패해도 나머지 필드와 기본 조회는 그대로 성공해야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderQueryServiceTest {

	@Mock
	private OrderQueryRepository orderQueryRepository;

	@Mock
	private OrderAccessPolicy orderAccessPolicy;

	@Mock
	private InventoryQueryRepository inventoryQueryRepository;

	@Mock
	private ProductPort productPort;

	@Mock
	private CompanyPort companyPort;

	@Mock
	private UserPort userPort;

	@InjectMocks
	private OrderQueryService orderQueryService;

	private final UUID orderId = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();
	private final UUID supplierCompanyId = UUID.randomUUID();
	private final UUID receiverCompanyId = UUID.randomUUID();
	private final UUID requesterUserId = UUID.randomUUID();
	private final UUID originHubId = UUID.randomUUID();
	private final UUID destHubId = UUID.randomUUID();

	private Order order;

	private final UUID inventoryId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		order = Order.builder()
				.supplierCompanyId(supplierCompanyId)
				.receiverCompanyId(receiverCompanyId)
				.receiverUserId(requesterUserId)
				.requestDetails("오전 중 배송 부탁드립니다")
				.build();
		order.addItem(OrderItem.builder()
				.productId(productId)
				.quantity(5)
				.inventoryId(inventoryId)
				.build());

		given(orderQueryRepository.findDetailById(orderId)).willReturn(Optional.of(order));

		given(productPort.getProductInfo(productId))
				.willReturn(new ProductPort.ProductInfo(productId, "햇사과 10kg 박스"));
		given(companyPort.getCompanyInfo(supplierCompanyId))
				.willReturn(new CompanyPort.CompanyInfo(supplierCompanyId, "이천 농산물 공급", UUID.randomUUID()));
		given(companyPort.getCompanyInfo(receiverCompanyId))
				.willReturn(new CompanyPort.CompanyInfo(receiverCompanyId, "강남 유통 마트", destHubId));

		Inventory inventory = Inventory.builder()
				.productId(productId)
				.hubId(originHubId)
				.quantity(100)
				.build();
		given(inventoryQueryRepository.findDetailById(inventoryId))
				.willReturn(Optional.of(inventory));
	}

	@Test
	@DisplayName("include를 안 줘도 상품명·업체명·허브ID는 항상 채워진다")
	void alwaysEnrichesProductAndCompanyAndHubIds() {
		// when
		OrderInternalDetailResult result = orderQueryService.getOrderForInternal(orderId, Set.of());

		// then
		assertThat(result.productName()).isEqualTo("햇사과 10kg 박스");
		assertThat(result.supplierCompanyName()).isEqualTo("이천 농산물 공급");
		assertThat(result.receiverCompanyName()).isEqualTo("강남 유통 마트");
		assertThat(result.originHubId()).isEqualTo(originHubId);
		assertThat(result.destHubId()).isEqualTo(destHubId);
		assertThat(result.requesterName()).isNull();
	}

	@Test
	@DisplayName("include=requester일 때만 요청자명을 조회한다")
	void resolvesRequesterNameOnlyWhenIncluded() {
		// given
		given(userPort.getReceiver(requesterUserId))
				.willReturn(new UserPort.Receiver(requesterUserId, "홍길동", null, null));

		// when
		OrderInternalDetailResult result =
				orderQueryService.getOrderForInternal(orderId, Set.of("requester"));

		// then
		assertThat(result.requesterName()).isEqualTo("홍길동");
	}

	@Test
	@DisplayName("상품명 조회가 실패해도 나머지 필드는 정상 반환된다")
	void toleratesProductLookupFailure() {
		// given
		given(productPort.getProductInfo(productId))
				.willThrow(new RuntimeException("company-service 장애"));

		// when
		OrderInternalDetailResult result = orderQueryService.getOrderForInternal(orderId, Set.of());

		// then
		assertThat(result.productName()).isNull();
		assertThat(result.supplierCompanyName()).isEqualTo("이천 농산물 공급");
		assertThat(result.originHubId()).isEqualTo(originHubId);
	}

	@Test
	@DisplayName("include=requester인데 사용자 조회가 실패해도 200 흐름을 유지하고 requesterName만 비어 있다")
	void toleratesRequesterLookupFailure() {
		// given
		given(userPort.getReceiver(requesterUserId))
				.willThrow(new RuntimeException("user-service 장애"));

		// when
		OrderInternalDetailResult result =
				orderQueryService.getOrderForInternal(orderId, Set.of("requester"));

		// then
		assertThat(result.requesterName()).isNull();
		assertThat(result.productName()).isEqualTo("햇사과 10kg 박스");
	}
}
