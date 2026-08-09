package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 주문 내부 API — slack-service 의 AI 파트가 쓰는 계약을 고정한다. */
@WebMvcTest(OrderInternalController.class)
class OrderInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderQueryService orderQueryService;

	private final UUID orderId = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();

	@Test
	@DisplayName("AI 가 필요한 주문 정보만 반환하고 상태·감사 필드는 내보내지 않는다")
	void getOrderForInternal() throws Exception {
		// given
		UUID supplierCompanyId = UUID.randomUUID();
		UUID receiverCompanyId = UUID.randomUUID();
		given(orderQueryService.getOrderForInternal(any()))
				.willReturn(new OrderInternalDetailResult(
						orderId, supplierCompanyId, receiverCompanyId, "오전 중 배송 부탁드립니다",
						List.of(new OrderInternalDetailResult.Item(productId, 50))));

		// when & then
		mockMvc.perform(get("/internal/v1/orders/{orderId}", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.supplierCompanyId").value(supplierCompanyId.toString()))
				.andExpect(jsonPath("$.data.receiverCompanyId").value(receiverCompanyId.toString()))
				.andExpect(jsonPath("$.data.requestDetails").value("오전 중 배송 부탁드립니다"))
				.andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
				.andExpect(jsonPath("$.data.items[0].quantity").value(50))
				// 주문 진행 상태와 감사 필드는 order 내부 관심사라 내보내지 않는다
				.andExpect(jsonPath("$.data.status").doesNotExist())
				.andExpect(jsonPath("$.data.createdBy").doesNotExist());
	}
}
