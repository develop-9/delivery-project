package com.delivery_project.order_service.global.exception;

import com.delivery_project.order_service.global.security.UserContextInterceptor;
import com.delivery_project.order_service.order.application.command_service.OrderCommandService;
import com.delivery_project.order_service.order.application.query_service.OrderQueryService;
import com.delivery_project.order_service.order.presentation.api_controller.OrderApiController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderApiController.class)
@Import(UserContextInterceptor.class)   // WebConfig 가 참조하는 인터셉터는 슬라이스에 자동 포함되지 않는다
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderCommandService orderCommandService;

	@MockitoBean
	private OrderQueryService orderQueryService;

	@Test
	@DisplayName("경로변수가 빈 요청(끝 슬래시)은 500 이 아니라 404 를 돌려준다")
	void returnsNotFoundOnTrailingSlash() throws Exception {
		mockMvc.perform(get("/api/v1/orders/"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.errorCode").value("NOT_FOUND"));
	}

	@Test
	@DisplayName("매핑되지 않은 경로도 공통 에러 응답 형식으로 404 를 돌려준다")
	void returnsNotFoundOnUnmappedPath() throws Exception {
		mockMvc.perform(get("/api/v1/nonexistent"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.errorCode").value("NOT_FOUND"));
	}

	@Test
	@DisplayName("경로변수 형식이 잘못되면 400 을 돌려준다")
	void returnsBadRequestOnInvalidPathVariable() throws Exception {
		mockMvc.perform(get("/api/v1/orders/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.errorCode").value("INVALID_REQUEST"));
	}
}
