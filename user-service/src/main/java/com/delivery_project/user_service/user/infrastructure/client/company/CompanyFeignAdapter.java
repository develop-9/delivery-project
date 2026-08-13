package com.delivery_project.user_service.user.infrastructure.client.company;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.port.CompanyPort;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyFeignAdapter implements CompanyPort {

	private final CompanyClient companyClient;

	@Override
	public void validateExists(UUID companyId) {
		try {
			companyClient.getCompany(companyId);
		} catch (FeignException.NotFound e) {
			log.info("[User] 존재하지 않는 업체 companyId={}", companyId);
			throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
		} catch (FeignException e) {
			log.warn("[User] Company Service 연동 실패 companyId={}", companyId, e);
			throw new BusinessException(ErrorCode.COMPANY_SERVICE_UNAVAILABLE);
		}
	}
}
