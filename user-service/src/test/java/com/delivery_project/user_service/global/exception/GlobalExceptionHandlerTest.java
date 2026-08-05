package com.delivery_project.user_service.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import com.delivery_project.user_service.global.response.ErrorResponse;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void BusinessException은_ErrorCode에_맞는_상태코드와_에러바디로_변환된다() {
		// given
		BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

		// when
		ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().error().errorCode()).isEqualTo("USER_NOT_FOUND");
		assertThat(response.getBody().error().message()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
	}

	@Test
	void IllegalStateException은_INVALID_STATE로_변환된다() {
		// given
		IllegalStateException exception = new IllegalStateException("이미 삭제된 엔티티입니다.");

		// when
		ResponseEntity<ErrorResponse> response = handler.handleIllegalStateException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().error().errorCode()).isEqualTo("INVALID_STATE");
	}

	@Test
	void NoSuchElementException은_NOT_FOUND로_변환된다() {
		// given
		NoSuchElementException exception = new NoSuchElementException();

		// when
		ResponseEntity<ErrorResponse> response = handler.handleNoSuchElementException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().error().errorCode()).isEqualTo("NOT_FOUND");
	}

	@Test
	void AccessDeniedException은_AUTH_FORBIDDEN으로_변환된다() {
		// given
		AccessDeniedException exception = new AccessDeniedException("접근 거부");

		// when
		ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody().error().errorCode()).isEqualTo("AUTH_FORBIDDEN");
	}

	@Test
	void ValidationException은_INVALID_INPUT_VALUE와_필드_에러_목록으로_변환된다() {
		// given
		BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userSignupRequest");
		bindingResult.addError(new FieldError(
				"userSignupRequest", "username", "invalid-user!!", false, null, null,
				"아이디는 4~10자의 소문자와 숫자로만 구성되어야 합니다."));
		BindException exception = new BindException(bindingResult);

		// when
		ResponseEntity<ErrorResponse> response = handler.handleBindException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().error().errorCode()).isEqualTo("INVALID_INPUT_VALUE");
		assertThat(response.getBody().error().fieldErrors()).hasSize(1);
		assertThat(response.getBody().error().fieldErrors().get(0).field()).isEqualTo("username");
		assertThat(response.getBody().error().fieldErrors().get(0).rejectedValue()).isEqualTo("invalid-user!!");
		assertThat(response.getBody().error().fieldErrors().get(0).reason())
				.isEqualTo("아이디는 4~10자의 소문자와 숫자로만 구성되어야 합니다.");
	}

	@Test
	void 필수_쿼리_파라미터가_누락되면_INVALID_INPUT_VALUE로_변환되고_필드_에러는_없다() {
		// given
		org.springframework.web.bind.MissingServletRequestParameterException exception =
				new org.springframework.web.bind.MissingServletRequestParameterException("hubId", "UUID");

		// when
		ResponseEntity<ErrorResponse> response = handler.handleInvalidInputValueException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().error().errorCode()).isEqualTo("INVALID_INPUT_VALUE");
		assertThat(response.getBody().error().fieldErrors()).isNull();
	}

	@Test
	void 형식이_잘못된_요청은_INVALID_REQUEST로_변환된다() {
		// given
		HttpMessageNotReadableException exception = org.mockito.Mockito.mock(HttpMessageNotReadableException.class);

		// when
		ResponseEntity<ErrorResponse> response = handler.handleInvalidRequestException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().error().errorCode()).isEqualTo("INVALID_REQUEST");
	}

	@Test
	void DB_제약_위반은_INVALID_STATE로_변환된다() {
		// given
		DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key value violates unique constraint");

		// when
		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().error().errorCode()).isEqualTo("INVALID_STATE");
	}

	@Test
	void 지원하지_않는_미디어타입은_UNSUPPORTED_MEDIA_TYPE으로_변환된다() {
		// given
		HttpMediaTypeNotSupportedException exception = org.mockito.Mockito.mock(HttpMediaTypeNotSupportedException.class);

		// when
		ResponseEntity<ErrorResponse> response = handler.handleHttpMediaTypeNotSupportedException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertThat(response.getBody().error().errorCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
	}

	@Test
	void 예상하지_못한_예외는_500과_INTERNAL_SERVER_ERROR로_변환된다() {
		// given
		RuntimeException exception = new RuntimeException("boom");

		// when
		ResponseEntity<ErrorResponse> response = handler.handleException(exception);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().error().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
	}
}
