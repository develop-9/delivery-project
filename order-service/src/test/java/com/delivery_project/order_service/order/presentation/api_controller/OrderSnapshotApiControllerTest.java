package com.delivery_project.order_service.order.presentation.api_controller;

import com.delivery_project.order_service.order.application.query_service.OrderSnapshotQueryService;
import com.delivery_project.order_service.order.application.result.OrderSnapshotResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.delivery_project.order_service.global.security.JwtAuthenticationFilter;
import com.delivery_project.order_service.global.security.JwtTokenParser;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주문 이력 조회 엔드포인트 검증.
 *
 * <p>API 명세와 실제 경로가 어긋났던 이력이 있어(PR 리뷰 P1) <b>경로 자체를 테스트로 고정</b>한다.
 * 타임라인은 주문 하위 리소스, 단건은 독립 리소스라 두 경로가 다른 계층에 있다.
 */
@WebMvcTest(OrderSnapshotApiController.class)
@Import({JwtAuthenticationFilter.class, JwtTokenParser.class})
// 컨트롤러 동작만 본다. 인증 자체는 JwtAuthenticationFilterTest 가 검증한다
@AutoConfigureMockMvc(addFilters = false)
class OrderSnapshotApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderSnapshotQueryService orderSnapshotQueryService;

	private final UUID orderId = UUID.randomUUID();
	private final UUID orderSnapshotId = UUID.randomUUID();

	private OrderSnapshotResult result() {
		return new OrderSnapshotResult(
				orderSnapshotId, orderId, 1, "ORDER_CREATED", "PENDING",
				null, null, null, null, List.of(), "오전 중 배송 부탁드립니다",
				Instant.now(), UUID.randomUUID());
	}

	@Test
	@DisplayName("타임라인은 주문 하위 경로(/api/v1/orders/{orderId}/order-snapshots)로 매핑된다")
	void timelineIsNestedUnderOrder() throws Exception {
		// given
		given(orderSnapshotQueryService.getSnapshots(any(), any(), any(), any()))
				.willReturn(emptyPage());

		// when & then
		mockMvc.perform(get("/api/v1/orders/{orderId}/order-snapshots", orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("단건 조회는 독립 경로(/api/v1/order-snapshots/{orderSnapshotId})로 매핑된다")
	void detailIsStandaloneResource() throws Exception {
		// given
		given(orderSnapshotQueryService.getSnapshot(any(), any())).willReturn(result());

		// when & then
		mockMvc.perform(get("/api/v1/order-snapshots/{orderSnapshotId}", orderSnapshotId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.snapshotId").value(orderSnapshotId.toString()))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()));
	}

	@Test
	@DisplayName("주문 하위에 중첩된 단건 경로는 더 이상 존재하지 않는다")
	void nestedDetailPathIsGone() throws Exception {
		// given & when & then — 명세 이전 경로. 남아 있으면 명세 불일치가 되살아난 것이다.
		mockMvc.perform(get("/api/v1/orders/{orderId}/order-snapshots/{orderSnapshotId}", orderId, orderSnapshotId))
				.andExpect(status().isNotFound());
	}

	/** 타임라인 반환용 빈 페이지 */
	private Page<OrderSnapshotResult> emptyPage() {
		return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
	}
}
