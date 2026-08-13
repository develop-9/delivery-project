package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.delivery_project.order_service.global.security.JwtAuthenticationFilter;
import com.delivery_project.order_service.global.security.JwtTokenParser;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 주문 내부 API — slack-service 의 AI 파트가 쓰는 계약을 고정한다. */
@WebMvcTest(OrderInternalController.class)
@Import({JwtAuthenticationFilter.class, JwtTokenParser.class})
// 컨트롤러 동작만 본다. 인증 자체는 JwtAuthenticationFilterTest 가 검증한다
@AutoConfigureMockMvc(addFilters = false)
class OrderInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderQueryService orderQueryService;

	@MockitoBean
	private OrderCommandService orderCommandService;

	private final UUID orderId = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();

	@Test
	@DisplayName("AI/Delivery 가 필요한 주문 정보를 반환한다 — include 없으면 requesterName은 비어 있다")
	void getOrderForInternal() throws Exception {
		// given
		UUID supplierCompanyId = UUID.randomUUID();
		UUID receiverCompanyId = UUID.randomUUID();
		UUID originHubId = UUID.randomUUID();
		UUID destHubId = UUID.randomUUID();
		Instant createdAt = Instant.now();

		given(orderQueryService.getOrderForInternal(eq(orderId), eq(Set.of())))
				.willReturn(new OrderInternalDetailResult(
						orderId, "CONFIRMED", productId, "마른 오징어", 50,
						supplierCompanyId, "일산 건조식품 가공",
						receiverCompanyId, "부산 수산물 도매",
						originHubId, destHubId,
						UUID.randomUUID(), null,
						"오전 중 배송 부탁드립니다", createdAt,
						List.of(new OrderInternalDetailResult.Item(productId, 50))));

		// when & then
		mockMvc.perform(get("/internal/v1/orders/{orderId}", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.data.productName").value("마른 오징어"))
				.andExpect(jsonPath("$.data.supplierCompanyId").value(supplierCompanyId.toString()))
				.andExpect(jsonPath("$.data.supplierCompanyName").value("일산 건조식품 가공"))
				.andExpect(jsonPath("$.data.receiverCompanyId").value(receiverCompanyId.toString()))
				.andExpect(jsonPath("$.data.originHubId").value(originHubId.toString()))
				.andExpect(jsonPath("$.data.destHubId").value(destHubId.toString()))
				.andExpect(jsonPath("$.data.requesterName").doesNotExist())
				.andExpect(jsonPath("$.data.requestDetails").value("오전 중 배송 부탁드립니다"))
				.andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
				.andExpect(jsonPath("$.data.items[0].quantity").value(50));
	}

	@Test
	@DisplayName("include=requester를 주면 그 값 그대로 서비스에 전달한다")
	void getOrderForInternalWithInclude() throws Exception {
		// given
		given(orderQueryService.getOrderForInternal(eq(orderId), eq(Set.of("requester"))))
				.willReturn(new OrderInternalDetailResult(
						orderId, "CONFIRMED", productId, "마른 오징어", 50,
						UUID.randomUUID(), null, UUID.randomUUID(), null,
						null, null, UUID.randomUUID(), "홍길동",
						null, Instant.now(), List.of()));

		// when & then
		mockMvc.perform(get("/internal/v1/orders/{orderId}", orderId)
						.param("include", "requester"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.requesterName").value("홍길동"));

		then(orderQueryService).should().getOrderForInternal(orderId, Set.of("requester"));
	}

	@Test
	@DisplayName("배송 완료 통보를 받으면 주문 완료 처리로 넘긴다")
	void completeOrder() throws Exception {
		// when & then — 통보에 대한 응답이라 본문은 비운다
		mockMvc.perform(post("/internal/v1/orders/{orderId}/complete", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		then(orderCommandService).should().complete(orderId);
	}

	@Test
	@DisplayName("업체와 엮인 주문 ID 를 목록으로 돌려준다")
	void relatedOrderIds() throws Exception {
		// given
		UUID companyId = UUID.randomUUID();
		UUID otherOrderId = UUID.randomUUID();
		given(orderQueryService.getRelatedOrderIds(companyId))
				.willReturn(List.of(orderId, otherOrderId));

		// when & then
		mockMvc.perform(get("/internal/v1/orders/related-order-ids")
						.param("companyId", companyId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.orderIds.length()").value(2))
				.andExpect(jsonPath("$.data.orderIds[0]").value(orderId.toString()));
	}

	@Test
	@DisplayName("엮인 주문이 없으면 빈 배열이다 — 404 가 아니다")
	void relatedOrderIdsEmpty() throws Exception {
		// given
		UUID companyId = UUID.randomUUID();
		given(orderQueryService.getRelatedOrderIds(companyId)).willReturn(List.of());

		// when & then — 호출 측이 빈 결과로 다룰 수 있어야 한다
		mockMvc.perform(get("/internal/v1/orders/related-order-ids")
						.param("companyId", companyId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.orderIds").isEmpty());
	}
}
