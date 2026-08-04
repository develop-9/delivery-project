package com.delivery_project.user_service.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.delivery_project.user_service.global.response.CommonResponse;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void BusinessException은_ErrorCode에_맞는_상태코드와_에러바디로_변환된다() {
		// given
		BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

		// when
		ResponseEntity<CommonResponse<Void>> response = handler.handleBusinessException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().isSuccess()).isFalse();
		assertThat(response.getBody().getError().getErrorCode()).isEqualTo("USER_NOT_FOUND");
		assertThat(response.getBody().getError().getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
	}

	@Test
	void ValidationException은_INVALID_REQUEST로_변환된다() {
		// given
		MethodArgumentNotValidException exception = mockValidationException();

		// when
		ResponseEntity<CommonResponse<Void>> response = handler.handleValidationException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getError().getErrorCode()).isEqualTo("INVALID_REQUEST");
	}

	@Test
	void 예상하지_못한_예외는_500과_INTERNAL_SERVER_ERROR로_변환된다() {
		// given
		IllegalStateException exception = new IllegalStateException("boom");

		// when
		ResponseEntity<CommonResponse<Void>> response = handler.handleException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().getError().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
	}

	private MethodArgumentNotValidException mockValidationException() {
		return org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
	}
}
