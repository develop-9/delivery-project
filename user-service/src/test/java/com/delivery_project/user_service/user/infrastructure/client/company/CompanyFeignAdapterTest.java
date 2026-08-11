package com.delivery_project.user_service.user.infrastructure.client.company;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;

@ExtendWith(MockitoExtension.class)
class CompanyFeignAdapterTest {

	@Mock
	private CompanyClient companyClient;

	@InjectMocks
	private CompanyFeignAdapter companyFeignAdapter;

	@Test
	void 존재하는_업체면_예외없이_반환한다() {
		// given
		UUID companyId = UUID.randomUUID();

		// when & then
		assertThatCode(() -> companyFeignAdapter.validateExists(companyId))
				.doesNotThrowAnyException();
	}

	@Test
	void 존재하지_않는_업체면_COMPANY_NOT_FOUND로_변환한다() {
		// given
		UUID companyId = UUID.randomUUID();
		Request request = fakeGetRequest(companyId);
		Mockito.doThrow(new FeignException.NotFound("Not Found", request, null, Collections.emptyMap()))
				.when(companyClient).getCompany(companyId);

		// when & then
		assertThatThrownBy(() -> companyFeignAdapter.validateExists(companyId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
	}

	@Test
	void 그_외_실패는_COMPANY_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		UUID companyId = UUID.randomUUID();
		Request request = fakeGetRequest(companyId);
		Mockito.doThrow(new RetryableException(
						503, "Read timed out", Request.HttpMethod.GET, (Long) null, request))
				.when(companyClient).getCompany(companyId);

		// when & then
		assertThatThrownBy(() -> companyFeignAdapter.validateExists(companyId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.COMPANY_SERVICE_UNAVAILABLE);
	}

	private Request fakeGetRequest(UUID companyId) {
		return Request.create(
				"GET", "/internal/v1/companies/" + companyId,
				Collections.emptyMap(), null, (Charset) null);
	}
}
