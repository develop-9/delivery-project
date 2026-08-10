package com.delivery_project.user_service.user.application.port;

import java.util.UUID;

/**
 * 존재하지 않는 업체면 BusinessException(COMPANY_NOT_FOUND, 404)이,
 * Company Service 연동 실패면 BusinessException(COMPANY_SERVICE_UNAVAILABLE, 503)이 던져진다.
 */
public interface CompanyPort {

	void validateExists(UUID companyId);
}
