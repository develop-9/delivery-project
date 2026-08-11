package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.order.application.command_service.InventoryCommandService;
import com.delivery_project.order_service.order.application.query_service.InventoryQueryService;
import com.delivery_project.order_service.order.application.result.InventoryInternalDeleteResult;
import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.delivery_project.order_service.global.security.JwtAuthenticationFilter;
import com.delivery_project.order_service.global.security.JwtTokenParser;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 재고 내부 API — company-service 가 쓰는 계약을 고정한다. */
@WebMvcTest(InventoryInternalController.class)
@Import({JwtAuthenticationFilter.class, JwtTokenParser.class})
// 컨트롤러 동작만 본다. 인증 자체는 JwtAuthenticationFilterTest 가 검증한다
@AutoConfigureMockMvc(addFilters = false)
class InventoryInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private InventoryCommandService inventoryCommandService;

	@MockitoBean
	private InventoryQueryService inventoryQueryService;

	private final UUID productId = UUID.randomUUID();
	private final UUID hubId = UUID.randomUUID();
	private final UUID companyId = UUID.randomUUID();
	private final UUID inventoryId = UUID.randomUUID();

	@Test
	@DisplayName("초기 재고 레코드를 만들면 201 과 수량 0 을 돌려준다")
	void createInitial() throws Exception {
		// given
		given(inventoryCommandService.createInitial(any()))
				.willReturn(List.of(new InventoryInternalSummaryResult(
						inventoryId, productId, hubId, 0, 0, Instant.now())));

		String body = objectMapper.writeValueAsString(Map.of("productId", productId));

		// when & then
		mockMvc.perform(post("/internal/v1/inventories")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				// company 의 InventorySaveFeignResponse 와 필드명이 맞아야 한다
				.andExpect(jsonPath("$.data.inventoryList[0].inventoryId").value(inventoryId.toString()))
				.andExpect(jsonPath("$.data.inventoryList[0].createdAt").exists());
	}

	@Test
	@DisplayName("필수값이 빠지면 400 을 돌려준다")
	void createInitialWithoutProductId() throws Exception {
		// given — productId 누락
		String body = objectMapper.writeValueAsString(Map.of());

		// when & then
		mockMvc.perform(post("/internal/v1/inventories")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@DisplayName("상품 재고 일괄 삭제는 지운 목록을 돌려준다")
	void deleteByProduct() throws Exception {
		// given
		given(inventoryCommandService.deleteByProduct(any(), any()))
				.willReturn(List.of(new InventoryInternalDeleteResult(
						inventoryId, hubId, 30, Instant.now())));

		// when & then — company 의 InventoryDeleteFeignResponse 와 필드명이 맞아야 한다
		mockMvc.perform(delete("/internal/v1/inventories/{productId}", productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.inventoryList[0].inventoryId").value(inventoryId.toString()))
				.andExpect(jsonPath("$.data.inventoryList[0].remainingQuantity").value(30))
				.andExpect(jsonPath("$.data.inventoryList[0].deletedAt").exists());
	}

	@Test
	@DisplayName("지울 재고가 없어도 오류가 아니다")
	void deleteByProductWithoutInventory() throws Exception {
		// given — 재고가 만들어지기 전에 상품이 지워졌을 수 있다
		given(inventoryCommandService.deleteByProduct(any(), any())).willReturn(List.of());

		// when & then
		mockMvc.perform(delete("/internal/v1/inventories/{productId}", productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.inventoryList").isEmpty());
	}

	@Test
	@DisplayName("상품별 허브 재고를 목록으로 돌려주고 선점 수량은 내보내지 않는다")
	void getByProduct() throws Exception {
		// given
		given(inventoryQueryService.getInventoriesByProduct(productId))
				.willReturn(List.of(new InventoryInternalSummaryResult(
						inventoryId, productId, hubId, 100, 70, Instant.now())));

		// when & then
		mockMvc.perform(get("/internal/v1/inventories").param("productId", productId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.inventoryList[0].hubId").value(hubId.toString()))
				.andExpect(jsonPath("$.data.inventoryList[0].quantity").value(100))
				.andExpect(jsonPath("$.data.inventoryList[0].availableQuantity").value(70))
				// 선점은 order 내부 사정이라 내보내지 않는다
				.andExpect(jsonPath("$.data.inventoryList[0].reservedQuantity").doesNotExist());
	}
}
