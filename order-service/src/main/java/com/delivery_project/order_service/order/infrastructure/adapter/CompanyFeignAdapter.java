package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.port.CompanyPort;
import com.delivery_project.order_service.order.infrastructure.client.CompanyInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.dto.CompanyInfoResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyFeignAdapter implements CompanyPort {

	private final CompanyInternalClient companyInternalClient;

	@Override
	public ReceiverCompany getReceiverCompany(UUID companyId) {
		CompanyInfoResponse company = call(companyId);

		// 상대 응답에 필드가 아직 없으면 여기서 걸린다. 배송 생성 시점에 터지면 원인이 안 보인다.
		if (company.hubId() == null || company.address() == null) {
			log.error("[업체] 배송 생성에 필요한 값 누락 : [{}] hubId={} address={}",
					companyId, company.hubId(), company.address());
			throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
					"수령 업체의 허브·주소 정보를 가져올 수 없습니다.");
		}

		return new ReceiverCompany(companyId, company.hubId(), company.address());
	}

	@Override
	public CompanyInfo getCompanyInfo(UUID companyId) {
		CompanyInfoResponse company = call(companyId);
		return new CompanyInfo(companyId, company.name(), company.hubId());
	}

	private CompanyInfoResponse call(UUID companyId) {
		try {
			InternalApiResponse<CompanyInfoResponse> response =
					companyInternalClient.getCompany(companyId);

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
						"업체 정보를 가져올 수 없습니다.");
			}
			return response.data();

		} catch (FeignException.NotFound exception) {
			log.warn("[업체] 존재하지 않는 업체 : [{}]", companyId);
			throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND);

		} catch (FeignException exception) {
			log.error("[업체] 조회 실패 : [{}] status={}", companyId, exception.status(), exception);
			throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
					"업체 서비스를 사용할 수 없습니다.");
		}
	}
}
