package com.delivery_project.order_service.order.presentation.internal_controller;

import com.delivery_project.order_service.order.application.command_service.InventoryCommandService;
import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 재고 내부 API — company-service 가 쓰는 계약을 고정한다. */
@WebMvcTest(InventoryInternalController.class)
class InventoryInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private InventoryCommandService inventoryCommandService;

	private final UUID productId = UUID.randomUUID();
	private final UUID hubId = UUID.randomUUID();
	private final UUID companyId = UUID.randomUUID();

	@Test
	@DisplayName("초기 재고 레코드를 만들면 201 과 수량 0 을 돌려준다")
	void createInitial() throws Exception {
		// given
		UUID inventoryId = UUID.randomUUID();
		given(inventoryCommandService.createInitial(any()))
				.willReturn(new InventoryInternalSummaryResult(inventoryId, productId, hubId, 0, 0));

		String body = objectMapper.writeValueAsString(
				Map.of("productId", productId, "hubId", hubId, "companyId", companyId));

		// when & then
		mockMvc.perform(post("/internal/v1/inventories")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.inventoryId").value(inventoryId.toString()))
				.andExpect(jsonPath("$.data.quantity").value(0))
				.andExpect(jsonPath("$.data.availableQuantity").value(0));
	}

	@Test
	@DisplayName("필수값이 빠지면 400 을 돌려준다")
	void createInitialWithoutProductId() throws Exception {
		// given — productId 누락
		String body = objectMapper.writeValueAsString(Map.of("hubId", hubId, "companyId", companyId));

		// when & then
		mockMvc.perform(post("/internal/v1/inventories")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

}
